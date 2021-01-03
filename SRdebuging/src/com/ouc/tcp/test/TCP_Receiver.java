/***************************2.1: ACK/NACK*****************/
/***** Feng Hong; 2015-12-09******************************/
package com.ouc.tcp.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import com.ouc.tcp.client.TCP_Receiver_ADT;
import com.ouc.tcp.message.*;
import com.ouc.tcp.tool.TCP_TOOL;

public class TCP_Receiver extends TCP_Receiver_ADT {
	
	private TCP_PACKET ackPack;	//回复的ACK报文段
	int sequence=1;//记录期望收到的报文段号，注意包序号不完全是
	////利用hashMap存放收到的包
	private HashMap<Integer, TCP_PACKET> packets = 
			new HashMap<Integer, TCP_PACKET>();
	
	/*构造函数*/
	public TCP_Receiver() {
		super();	//调用超类构造函数
		super.initTCP_Receiver(this);	//初始化TCP接收端
	}

	@Override
	//接收到数据报：检查校验和，设置回复的ACK报文段
	public void rdt_recv(TCP_PACKET recvPack) {
		//检查校验码，生成ACK
		if(CheckSum.computeChkSum(recvPack) == recvPack.getTcpH().getTh_sum()) {//recvPack.getTcpH().getTh_sum()
			//打印一下checksum
			System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
			System.out.println("Recieved Packet: "+recvPack.getTcpH().getTh_sum());
			//生成ACK报文段（设置确认号）
			tcpH.setTh_ack(recvPack.getTcpH().getTh_seq());//此处设置的ACK号就是序列号,如果收到重复ack，还是会发重复包的序列号过去
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
			//回复ACK报文段
			reply(ackPack);
			
			int recvSeq = recvPack.getTcpH().getTh_seq();
			//如果需要这个包就把它留下来
			if (recvSeq >= sequence && !packets.containsKey(recvSeq)) {
				packets.put(recvSeq, recvPack);
			}

			//将接收到的数据有序地插入data队列，准备交付，注意不可以使data队列中的长度超过20
			while(packets.containsKey(sequence) && dataQueue.size() < 20) {
				TCP_PACKET packet = packets.remove(sequence);
				sequence += packet.getTcpS().getData().length;
				dataQueue.add(packet.getTcpS().getData());
			}
			
//			
//			//这就相当于扩大化的0/1状态
//			if(recvPack.getTcpH().getTh_seq()!=sequence){
//				//将接收到的正确有序的数据插入data队列，准备交付
//				dataQueue.add(recvPack.getTcpS().getData());				
//				//sequence++;
//				sequence=recvPack.getTcpH().getTh_seq();
//				System.out.println("Received packet,sequence:"+ sequence);
//			}else{
//				//收到重复包，提示并给出重复包序号
//				System.out.println("Received repeated packet,sequence:" + sequence);
//			}
		}else{
			//校验失败
			System.out.println("Recieve Computed: "+CheckSum.computeChkSum(recvPack));
			System.out.println("Recieved Packet: "+recvPack.getTcpH().getTh_sum());
			System.out.println("校验失败");
			System.out.println("Problem: Packet Number: "+recvPack.getTcpH().getTh_seq()+" + InnerSeq:  "+sequence);
			tcpH.setTh_ack(-1);
			ackPack = new TCP_PACKET(tcpH, tcpS, recvPack.getSourceAddr());
			tcpH.setTh_sum(CheckSum.computeChkSum(ackPack));
			//回复ACK报文段
			reply(ackPack);
		}
		
		System.out.println();
		
		
		//交付数据（每20组数据交付一次）
		if(dataQueue.size() == 20) 
			deliver_data();	
	}

	@Override
	//交付数据（将数据写入文件）；不需要修改
	public void deliver_data() {
		//检查dataQueue，将数据写入文件
		File fw = new File("recvData.txt");
		BufferedWriter writer;
		
		try {
			writer = new BufferedWriter(new FileWriter(fw, true));
			
			//循环检查data队列中是否有新交付数据
			while(!dataQueue.isEmpty()) {
				int[] data = dataQueue.poll();
				
				//将数据写入文件
				for(int i = 0; i < data.length; i++) {
					writer.write(data[i] + "\n");
				}
				
				writer.flush();		//清空输出缓存
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	//回复ACK报文段
	public void reply(TCP_PACKET replyPack) {
		//设置错误控制标志
		//0.信道无差错
		//1.只出错
		//2.只丢包
		//3.只延迟
		//4.出错 / 丢包
		//5.出错 / 延迟
		//6.丢包 / 延迟
		//7.出错 / 丢包 / 延迟
		tcpH.setTh_eflag((byte)4);	//eFlag=0，信道无错误
				
		//发送数据报
		client.send(replyPack);
	}
	
}
