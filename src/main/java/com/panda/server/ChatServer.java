package com.panda.server;

import com.panda.protocol.IMDecoder;
import com.panda.protocol.IMEncoder;
import com.panda.server.handler.HttpServerHandler;
import com.panda.server.handler.TerminalServerHandler;
import com.panda.server.handler.WebSockServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChatServer {
    private int port = 10080;
    public void start (int port) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            /** 解析自定义协议 **/
                            pipeline.addLast(new IMDecoder()); //Inbound
                            pipeline.addLast(new IMEncoder()); //Outbound
                            pipeline.addLast(new TerminalServerHandler()); //Inbound

                            /** 解析Http请求 **/
                            pipeline.addLast(new HttpServerCodec()); //Outbound
                            //主要是将同一个http请求或响应的多个消息对象变成一个fullHttpRequest完整的消息对象
                            pipeline.addLast(new HttpObjectAggregator(64 * 1024)); //Inbounds
                            // 主要用于处理大数据流，比如一个1G大小的文件如果你直接传输肯定会撑爆jvm内存的，加上这个handler我们就不用考虑这个问题了
                            pipeline.addLast(new ChunkedWriteHandler()); // Inbound、Outbound
                            pipeline.addLast(new HttpServerHandler()); // Inbound

                            /** 解析WebSocket请求 **/
                            pipeline.addLast(new WebSocketServerProtocolHandler("/im")); //Inbound
                            pipeline.addLast(new WebSockServerHandler()); // Inbound
                        }
                    });
            ChannelFuture f = b.bind(this.port).sync();
            log.info("服务已启动，监听端口" + this.port);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void start() {
        start(this.port);
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            new ChatServer().start(Integer.valueOf(args[0]));
        } else {
            new ChatServer().start();
        }
    }
}
