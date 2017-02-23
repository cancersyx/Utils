package com.zsf.demotest.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by zsf on 2017/2/23.
 * 多线程下载工具类
 */

public class DownUtils {

    private String path;//定义下载资源的路径
    private String targetFile;//指定所下载的文件的保存位置
    private int threadNum;//定义使用多少条线程下载资源
    private DownThread[] threads;//定义下载的线程对象
    private int fileSize;//定义下载的文件的大小

    public DownUtils(String path,String targetFile,int threadNum){
        this.path = path;
        this.targetFile = targetFile;
        this.threadNum = threadNum;
        threads = new DownThread[threadNum];//初始化threads数组
    }

    public void download() throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5*1000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept","image/gif,image/jpeg,image/pjpeg,image/png,"
        + "application/x-shockwave-flash,application/xaml+xml,"
        + "application/vnd.ms-xpsdocument,appication/x-ms-xbap,"
        + "application/x-ms-application,application/vnd.ms-excel,"
        + "application/vnd.ms-powerpoint,application/msword,*/*");
        conn.setRequestProperty("Accept-Language","zh-CN");
        conn.setRequestProperty("Charset","UTF-8");
        conn.setRequestProperty("Connection","Keep-Alive");
        //得到文件大小
        fileSize = conn.getContentLength();
        conn.disconnect();
        int currentPartSize = fileSize / threadNum + 1;
        RandomAccessFile file = new RandomAccessFile(targetFile,"rw");
        //设置本地文件的大小
        file.setLength(fileSize);
        file.close();
        for (int i = 0; i < threadNum; i++) {
            //计算每条线程下载的开始位置
            int startPos = i * currentPartSize;
            //每条线程使用一个RandomAccessFile进行下载
            RandomAccessFile currentPart = new RandomAccessFile(targetFile,"rw");
            //定位该线程的下载位置
            currentPart.seek(startPos);
            //创建下载线程
            threads[i] = new DownThread(startPos,currentPartSize,currentPart);
            //启动下载
            threads[i].start();
        }
    }


    public double getCompleteRate(){

        //统计多条线程已经下载的总大小
        int sumSize = 0;
        for (int i = 0; i < threadNum; i++) {
            sumSize += threads[i].length;
        }
        return sumSize * 1.0 / fileSize;

    }

    /**
     * 内部类，run方法负责打开远程资源的输入流，并调用
     * skip(int)方法跳过指定数量的字节
     */
    private class DownThread extends Thread{

        private int startPos;//当前线程的下载位置
        private int currentPartSize;//当前线程负责下载的文件大小
        private RandomAccessFile currentPart;//当前线程需要下载的文件块
        public int length;//该线程已经下载的字节数
        public DownThread(int startPos,int currentPartSize,RandomAccessFile currentPart){
            this.startPos = startPos;
            this.currentPartSize = currentPartSize;
            this.currentPart = currentPart;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(path);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(5 * 1000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept","image/gif,image/jpeg,image/pjpeg,image/png,"
                        + "application/x-shockwave-flash,application/xaml+xml,"
                        + "application/vnd.ms-xpsdocument,appication/x-ms-xbap,"
                        + "application/x-ms-application,application/vnd.ms-excel,"
                        + "application/vnd.ms-powerpoint,application/msword,*/*");
                connection.setRequestProperty("Accept-Language","zh-CN");
                connection.setRequestProperty("Charset","UTF-8");
                InputStream is = connection.getInputStream();
                //跳过startPos个字节，表明该线程只下载自己负责的那部分
                skipFully(is,startPos);
                byte[] buffer = new byte[1024];
                int hasRead = 0;
                //读取网络数据，并写入本地
                while(length < currentPartSize && (hasRead = is.read(buffer)) > 0){
                    currentPart.write(buffer,0,hasRead);
                    //累计该线程下载的总大小
                    length += hasRead;
                }

                currentPart.close();
                is.close();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void skipFully(InputStream is, long bytes) throws IOException {
            long remainning = bytes;
            long len = 0;
            while(remainning > 0){
                len = is.skip(remainning);
                remainning -=len;
            }
        }
    }

}
