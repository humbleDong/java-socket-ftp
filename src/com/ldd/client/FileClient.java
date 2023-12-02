package com.ldd.client;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

/**
 * FileClient
 *
 */
public class FileClient {
    private static final String HOST = "127.0.0.1";
    private static final int TCP_PORT = 2021;//消息
    private static final int UDP_PORT = 2020;

    // 下载路径
    private static final String DOWNLOAD_PATH = System.getProperty("user.dir") + "/src/com/ldd/downloads";
    public static void main(String[] args) throws Exception {
        // TCP 客户端socket
        Socket tcpSocket = new Socket(HOST, TCP_PORT);
        OutputStream os = tcpSocket.getOutputStream();
        InputStream is = tcpSocket.getInputStream();
        DataOutputStream dos = new DataOutputStream(os);
        DataInputStream dis = new DataInputStream(is);

        Scanner sc = new Scanner(System.in);
        // 服务端返回connected，说明已连接
        if("connected".equals(dis.readUTF())){
            System.out.println("客户端：" + tcpSocket.getInetAddress().getHostAddress()
                    + ":客户端" + tcpSocket.getLocalPort() + ">连接成功");
            while(true) {
                String msg = sc.nextLine();
                dos.writeUTF(msg);
                dos.flush();

                String getMsg = dis.readUTF();
                if("bye".equals(getMsg)) {
                    System.out.println("客户端" + tcpSocket.getInetAddress().getHostAddress()
                            + ":客户端" + tcpSocket.getPort() + ">已离线");
                    dos.close();
                    dis.close();
                    tcpSocket.close();
                    break;
                    // get指令：开始读取
                } else if(getMsg.startsWith("reading start")) {
                    System.out.println(getMsg.substring(14));
                    System.out.println("开始接收文件：" + msg);
                    DatagramSocket datagramSocket = new DatagramSocket();
                    //下载至客户端指定目录下
                    String downloadPath = DOWNLOAD_PATH + "\\" + msg.split(" ")[1];
                    File file = new File(downloadPath);
//                    FileWriter fileWriter = new FileWriter(file);
//                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                    
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);

                    // 定义传输packet
                    byte[] localUdpPort = (datagramSocket.getLocalPort() + "").getBytes();
                    DatagramPacket outPacket = new DatagramPacket(localUdpPort, localUdpPort.length
                            , InetAddress.getLocalHost(), UDP_PORT);
                    datagramSocket.send(outPacket);

                    getMsg = dis.readUTF();
                    while(!"reading end".equals(getMsg)) {
                        // UDP一次最多传输64KB
                        byte[] bytes = new byte[1024 * 64];
                        // 定义接收packet
                        DatagramPacket inPacket = new DatagramPacket(bytes, bytes.length);
                        datagramSocket.receive(inPacket);
//                        String receiveStr = new String(bytes, 0, inPacket.getLength());
//                        bufferedWriter.write(receiveStr);
//                        bufferedWriter.flush();
                        bos.write(bytes ,0 , inPacket.getLength());
                        bos.flush();
                        getMsg = dis.readUTF();
                    }
                    if("reading end".equals(getMsg)) {
//                        bufferedWriter.close();
                        fos.close();
                        datagramSocket.close();
                        getMsg = "文件接收完毕";
                    }
                }
                System.out.println(getMsg);
            }
        }
    }
}
