package com.ldd.server;


import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FileServer
 */
public class FileServer {
    // 当前root路径
    private static String ROOT = "";
    private static final int TCP_PORT = 2021;
    private static final int UDP_PORT = 2020;

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.println("请定义你的服务器文件夹(Root)：（例如：C:/A/B...）");// test: testFiles
        while(true) {
            String rootStr = sc.nextLine();
            if(hasRoot(rootStr)) {
                ROOT = rootStr;
                break;
            } else {
                System.out.println("root无效，请重新输入：");
            }
        }

        System.out.println("---服务器已启动！---");
        // TCP 服务端
        ServerSocket tcpServer = new ServerSocket(TCP_PORT);
        // TCP 服务端socket
        Socket socket;
        // UDP 服务端
        DatagramSocket udpSocket = new DatagramSocket(UDP_PORT);
        //线程池
        ExecutorService executorService;
        //单个处理器线程池工作线程数目
        final int POOL_SIZE=4;
        //创建线程池
        //Runtime的availableProcessors()方法返回当前系统可用处理器的数目
        //由JVM根据系统的情况来决定线程的数量
        executorService= Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_SIZE);

        while(true){
            try {
                socket = tcpServer.accept();
                System.out.println("客户端" + socket.getInetAddress().getHostAddress()
                        + ":客户端" + socket.getPort() + ">连接成功");
                executorService.execute(new TcpHandler(socket, udpSocket, ROOT)); //把执行交给线程池来维护
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断 root是否存在
     * @param root
     * @return boolean
     */
    public static boolean hasRoot(String root) {
        return new File(root).exists();
    }
}
