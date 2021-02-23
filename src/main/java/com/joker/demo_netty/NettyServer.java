package com.joker.demo_netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @version 1.0.0
 * @ClassName NettyServer.java
 * @Package com.joker.demo_netty
 * @Author Joker
 * @Description 创建NettyServer，定义两个EventLoopGroup，bossGroup辅助客户端的tcp连接请求, workGroup负责与客户端之前的读写操作，需要说明的是，需要开启一个新的线程来执行netty server，要不然会阻塞主线程，到时候就无法调用项目的其他controller接口了。
 * @CreateTime 2021年02月23日 15:30:00
 */
@Component
public class NettyServer{

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);

    /**
     * webSocket协议名
     */
    private static final String WEBSOCKET_PROTOCOL = "WebSocket";

    /**
     * 端口号
     */
    @Value("${webSocket.netty.port}")
    private int port;

    /**
     * webSocket路径
     */
    @Value("${webSocket.netty.path}")
    private String webSocketPath;

    /**
     * 在Netty心跳检测中配置 - 读空闲超时时间设置
     */
    @Value("${webSocket.netty.readerIdleTime}")
    private long readerIdleTime;

    /**
     * 在Netty心跳检测中配置 - 写空闲超时时间设置
     */
    @Value("${webSocket.netty.writerIdleTime}")
    private long writerIdleTime;

    /**
     * 在Netty心跳检测中配置 - 读写空闲超时时间设置
     */
    @Value("${webSocket.netty.allIdleTime}")
    private long allIdleTime;

    @Autowired
    private WebSocketHandler webSocketHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;

    /**
     * 启动
     * @throws InterruptedException
     */
    private void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup();
        workGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();
        // bossGroup辅助客户端的tcp连接请求, workGroup负责与客户端之前的读写操作
        bootstrap.group(bossGroup,workGroup);
        // 设置NIO类型的channel
        bootstrap.channel(NioServerSocketChannel.class);
        // 设置监听端口
        bootstrap.localAddress(new InetSocketAddress(port));
        // 连接到达时会创建一个通道
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                // 心跳检测(一般情况第一个设置,如果超时了,则会调用userEventTriggered方法,且会告诉你超时的类型)
                ch.pipeline().addLast(new IdleStateHandler(readerIdleTime, writerIdleTime, allIdleTime, TimeUnit.MINUTES));
                // 流水线管理通道中的处理程序（Handler），用来处理业务
                // webSocket协议本身是基于http协议的，所以这边也要使用http编解码器
                ch.pipeline().addLast(new HttpServerCodec());
                ch.pipeline().addLast(new ObjectEncoder());
                // 以块的方式来写的处理器
                ch.pipeline().addLast(new ChunkedWriteHandler());
                /*
                    说明：
                    1、http数据在传输过程中是分段的，HttpObjectAggregator可以将多个段聚合
                    2、这就是为什么，当浏览器发送大量数据时，就会发送多次http请求
                 */
                ch.pipeline().addLast(new HttpObjectAggregator(8192));
                /*
                    说明：
                    1、对应webSocket，它的数据是以帧（frame）的形式传递
                    2、浏览器请求时 ws://localhost:58080/xxx 表示请求的uri
                    3、核心功能是将http协议升级为ws协议，保持长连接
                */
                ch.pipeline().addLast(new WebSocketServerProtocolHandler(webSocketPath, WEBSOCKET_PROTOCOL, true, 65536 * 10));
                // 自定义的handler，处理业务逻辑
                ch.pipeline().addLast(webSocketHandler);
            }
        });
        // 配置完成，开始绑定server，通过调用sync同步方法阻塞直到绑定成功
        ChannelFuture channelFuture = bootstrap.bind().sync();
        log.info("Server started and listen on:{}",channelFuture.channel().localAddress());
        // 对关闭通道进行监听
        channelFuture.channel().closeFuture().sync();
    }

    /**
     * 释放资源
     * @throws InterruptedException
     */
    @PreDestroy
    public void destroy() throws InterruptedException {
        if(bossGroup != null){
            bossGroup.shutdownGracefully().sync();
        }
        if(workGroup != null){
            workGroup.shutdownGracefully().sync();
        }
    }

    /**
     * 初始化(新线程开启)
     */
    @PostConstruct()
    public void init() {
        //需要开启一个新的线程来执行netty server 服务器
        new Thread(() -> {
            try {
                start();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
