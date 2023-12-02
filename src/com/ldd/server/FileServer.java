package com.ldd.server;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/***
 *
 * @author xx
 * Create on 2023/12/2
 */
public class FileServer {

    // 当前root路径
    private static String ROOT = "";
    private static final int TCP_COMMAND_PORT = 2021;
    private static final int UDP_GET_FILE_PORT = 2020;

    private static final int POOL_SIZE = 4;

    /**
     * Runtime的availableProcessors()方法返回当前系统可用处理器的数目
     * 由JVM根据系统的情况来决定线程的数量
     */
    private static final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOL_SIZE);

    public static void main(String[] args) throws Exception {
        ROOT = defineFileRootFolder();
        System.out.println("---服务器已启动！---");

        // TCP 服务端
        ServerSocket tcpServer = new ServerSocket(TCP_COMMAND_PORT);
        // UDP 服务端
        DatagramSocket udpSocket = new DatagramSocket(UDP_GET_FILE_PORT);

        while(true){
            try {
                Socket socket = tcpServer.accept();
                System.out.println("客户端：" + socket.getInetAddress().getHostAddress()
                        + "，客户端：" + socket.getPort() + " 连接成功！");
                executorService.execute(new TcpClientHandler(socket, udpSocket, ROOT)); //把执行交给线程池来维护
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /***
     * 定义FTP文件 ROOT folder
     */
    private static String defineFileRootFolder() {
        Scanner sc = new Scanner(System.in);
        // log4j
        System.out.println("请定义你的服务器文件夹(Root)：（例如：C:/A/B...）");// test: testFiles
        while(true) {
            String rootStr = sc.nextLine();
            if(hasRoot(rootStr)) {
                return rootStr;
            } else {
                System.out.println("root无效，请重新输入：");
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
