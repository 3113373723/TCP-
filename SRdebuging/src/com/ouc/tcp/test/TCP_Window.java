package com.ouc.tcp.test;

import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;

import com.ouc.tcp.message.TCP_PACKET;

public class TCP_Window implements Iterable<Integer> {

	//存放窗口中的包,关键字为包索引，值为包
	private ConcurrentSkipListMap<Integer, TCP_PACKET> packets = 
			new ConcurrentSkipListMap<Integer, TCP_PACKET>();
	
	private int length = 1; //窗口大小
	private int ssthresh = 16; //慢开始门限大小threshold,小于threshold指数增长，大于则线性增长
	private int nextIndex = 0; //下一个包的序号
		
	/**
	 * 把一个包加入窗口，请先检测窗口是否未满
	 * @param tcpPack 要加入窗口的包
	 */
	public void queuePacket(TCP_PACKET tcpPack) {
		packets.put(nextIndex, tcpPack);
		nextIndex += 1;
	}

	/**
	 * 收到了ACK包
	 * @param ack ACK号
	 */
	public void receiveACK(int ack) {
		//返回此映射中的键的可导航设置视图，即升序排列
		for(int i : packets.navigableKeySet()) {
			if(packets.get(i).getTcpH().getTh_seq() == ack) {
				//已被接收方接收确认，从发送包集合中移除
				packets.remove(i);
				//返回此映射中的键的可导航设置视图
				length = length < ssthresh ? length * 2 : length + 1;
			}
		}
	}

	/**
	 * 获取窗口是否为空
	 * @return 窗口是否为空
	 */
	public boolean isEmpty() {
		return packets.size() == 0;
	}

	/**
	 * 获取窗口是否已满
	 * @return 窗口是否已满
	 */
	public boolean isFull() {
		return !isEmpty() && (nextIndex - packets.firstKey()) >= length;
	}

	
	
	/*
	 * 获取迭代器
	 * return迭代器
	 * */@Override
	public Iterator<Integer> iterator() {
		// TODO Auto-generated method stub
		return packets.navigableKeySet().iterator();
	}

	/**
	 * 根据包的序号获取包
	 * @param index 包的序号
	 * @return 包
	 */
	public TCP_PACKET getPacket(int index) {
		return packets.get(index);
	}

	/**
	 * 通知窗口发生了拥塞
	 */
	public void congestionOccurred() {
		ssthresh = Math.max(length / 2, 2);
		length = 1;
	}
	 
	 
}
