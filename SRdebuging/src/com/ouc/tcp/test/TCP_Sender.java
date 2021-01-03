/***************************2.1: ACK/NACK
**************************** Feng Hong; 2015-12-09*/

package com.ouc.tcp.test;

import java.util.Timer;
import java.util.TimerTask;

import com.ouc.tcp.client.TCP_Sender_ADT;
import com.ouc.tcp.client.UDT_RetransTask;
import com.ouc.tcp.client.UDT_Timer;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Sender extends TCP_Sender_ADT {
	UDT_Timer timer;
	private TCP_PACKET tcpPack;	//待发送的TCP数据报
	//private volatile int flag = 0;
	
	private TCP_Window tcpWindow = new TCP_Window();//TCP窗口
	private int waitTime = 2000; //重发等待时间
	
	//多线程共享？
	private volatile boolean isTimeout = false; //是否等待ACK超时
	private class TimeoutTask extends TimerTask {
		@Override
		public void run() {
			isTimeout = true;
		}
	}

	
	
	private volatile boolean isNoMorePacket = false; //是否不再有新的包了
	private Timer noMorePacketTimer = new Timer();
	private class NoMorePacketTask extends TimerTask {
		@Override
		public void run() {
			isNoMorePacket = true;
			sendPacketsWhenNeedThenWaitACK();
		}
	}
	
	/*构造函数*/
	public TCP_Sender() {
		super();	//调用超类构造函数
		super.initTCP_Sender(this);		//初始化TCP发送端
	}
	
	//在应当发包的时候发送包并等待ACK
	private void sendPacketsWhenNeedThenWaitACK() {
		//窗口满了或者不再有新的包的时候就发送现有的包们
		while(tcpWindow.isFull() || (isNoMorePacket && !tcpWindow.isEmpty())) {
			//发送TCP数据报
			for(int i : tcpWindow) {
				udt_send(tcpWindow.getPacket(i));
			}
			//等待ACK报文
			waitACK();
		}
	}
	
	
	@Override
	//可靠发送（应用层调用）：封装应用层数据，产生TCP数据报；需要修改
	public void rdt_send(int dataIndex, int[] appData) {
		//还有新的包
		isNoMorePacket = false;
		noMorePacketTimer.cancel();
		noMorePacketTimer = new Timer();
		
		
		//生成TCP数据报（设置序号和数据字段/校验和),注意打包的顺序
		tcpH.setTh_seq(dataIndex * appData.length + 1);//包序号设置为字节流号：
		tcpS.setData(appData);
		tcpPack = new TCP_PACKET(tcpH, tcpS, destinAddr);		
	//	System.out.println("看看新生成的TCP包acksum的默认值:"+tcpPack.getTcpH().getTh_sum());		是0
		tcpH.setTh_sum(CheckSum.computeChkSum(tcpPack));
		
		tcpPack.setTcpH(tcpH);
		
		//把包放入TCP窗口
		tcpWindow.queuePacket(tcpPack);

		//发送窗口中的包
		sendPacketsWhenNeedThenWaitACK();

		//可能不会再有新的包了
		noMorePacketTimer.schedule(new NoMorePacketTask(), waitTime);
			
		//public void schedule(TimerTask task, long delay, long period)
		//调度一个task，在delay（ms）后开始调度，每次调度完后，最少等待period（ms）后才开始调度。
		//每隔3s执行重传，直到收到ACK
		//timer.schedule(reTrans, 3000, 3000);
	}
	
	@Override
	//不可靠发送：将打包好的TCP数据报通过不可靠传输信道发送；仅需修改错误标志
	public void udt_send(TCP_PACKET stcpPack) {
		//设置错误控制标志
		//0.信道无差错
		//1.只出错
		//2.只丢包
		//3.只延迟
		//4.出错 / 丢包
		//5.出错 / 延迟
		//6.丢包 / 延迟
		//7.出错 / 丢包 / 延迟
		tcpH.setTh_eflag((byte)4);		
		//System.out.println("to send: "+stcpPack.getTcpH().getTh_seq());				
		//发送数据报
		client.send(stcpPack);

	}
	
	@Override
	//需要修改
	public void waitACK() {
		//等待窗口内所有的包被确认或者超时
		isTimeout = false;
		UDT_Timer udtTimer = new UDT_Timer();
		udtTimer.schedule(new TimeoutTask(), waitTime, waitTime);
		while(!tcpWindow.isEmpty() && !isTimeout) ;
		udtTimer.cancel();

		if (isTimeout) {
			//发生了拥塞
			tcpWindow.congestionOccurred();
		}
	}

	@Override
	//接收到ACK报文：检查校验和，将确认号插入ack队列;NACK的确认号为－1；不需要修改
	public void recv(TCP_PACKET recvPack) {
		
		tcpWindow.receiveACK(recvPack.getTcpH().getTh_ack());
		System.out.println();
		
//		//检测收到的ACK包是否出错
//		if(CheckSum.computeChkSum(recvPack)==recvPack.getTcpH().getTh_sum()){
//			System.out.println("Receive ACK Number： "+ recvPack.getTcpH().getTh_ack());
//			ackQueue.add(recvPack.getTcpH().getTh_ack());
//		    System.out.println();	
//		}else{//ACK包出错，设为NACK，重发包
//			System.out.println("Receive Wrong ACK Packet! ");
//			ackQueue.add(-1);
//		    System.out.println();	
//		}
//	    //处理ACK报文
//	    //waitACK();
	   
	}
	
}
