package com.ouc.tcp.test;

import java.util.Arrays;
import java.util.zip.CRC32;
import com.ouc.tcp.message.TCP_HEADER;
import com.ouc.tcp.message.TCP_PACKET;

public class CheckSum {
	
	/*计算TCP报文段校验和：只需校验TCP首部中的seq、ack和sum，以及TCP数据字段*/
	public static short computeChkSum(TCP_PACKET tcpPack) {
		//二进制反码求和
		int checkSum = 0;
		//sum
//		checkSum+=tcpPack.getTcpH().getTh_sum();
//		checkSum=(checkSum>>16)+(checkSum&0xffff);
		//seq
		checkSum+=tcpPack.getTcpH().getTh_seq();
		checkSum=(checkSum>>16)+(checkSum&0xffff);
		//ack
		checkSum+=tcpPack.getTcpH().getTh_ack();
		checkSum=(checkSum>>16)+(checkSum&0xffff);
		
		//计算校验和
		int[] data=tcpPack.getTcpS().getData();
		for (int i = 0; i <data.length ; i++) {
			checkSum+=data[i];
			checkSum=(checkSum>>16)+(checkSum&0xffff);

		}
		checkSum+=(checkSum>>16);
//		checkSum=~checkSum;
		
		return (short) (~checkSum);
	}
	
}
