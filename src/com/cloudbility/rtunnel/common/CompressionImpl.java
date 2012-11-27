/*$Id: $
 --------------------------------------
  Skybility
 ---------------------------------------
  Copyright By Skybility ,All right Reserved
 * author   date   comment
 * jeremy  2012-9-17  Created
*/ 
package com.cloudbility.rtunnel.common; 

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZStream;
 
@SuppressWarnings("deprecation")
public class CompressionImpl implements Compression {
	  static private final int BUF_SIZE=4096;
//	  private final int buffer_margin=32+20; // AES256 + HMACSHA1
	  private int buffer_margin; // AES128 + packet header
	  private int type;
	  private ZStream stream;
	  private byte[] tmpbuf=new byte[BUF_SIZE];

	  public CompressionImpl(int buffer_margin){
	    stream=new ZStream();
	    this.buffer_margin=buffer_margin;
	  }

	  public void init(int type, int level){
	    if(type==DEFLATER){
	      stream.deflateInit(level);
	      this.type=DEFLATER;
	    }
	    else if(type==INFLATER){
	      stream.inflateInit();
	      inflated_buf=new byte[BUF_SIZE];
	      this.type=INFLATER;
	    }
	  }

	  private byte[] inflated_buf;
	  
	  public byte[] compress(byte[] buf, int start, int[] length){
	    stream.next_in=buf;
	    stream.next_in_index=start;
	    stream.avail_in=length[0];
	    int status;
	    int outputlen=start;
	    byte[] outputbuf=buf;
	    int tmp=0;

	    do{
	      stream.next_out=tmpbuf;
	      stream.next_out_index=0;
	      stream.avail_out=BUF_SIZE;
	      status=stream.deflate(JZlib.Z_PARTIAL_FLUSH);
	      switch(status){
	        case JZlib.Z_OK:
	        	//tmp 压缩数据的长度
	          tmp=BUF_SIZE-stream.avail_out;
	          if(outputbuf.length<outputlen+tmp+buffer_margin){
	            byte[] foo=new byte[(outputlen+tmp+buffer_margin)*2];
	            System.arraycopy(outputbuf, 0, foo, 0, outputbuf.length);
	            outputbuf=foo;
	          }
	          System.arraycopy(tmpbuf, 0, outputbuf, outputlen, tmp);
	          outputlen+=tmp;
	          break;
	        default:
		    System.err.println("compress: deflate returnd "+status);
	      }
	    }
	    while(stream.avail_out==0);
	    length[0]=outputlen-start;
	    return outputbuf;
	  }

	  public byte[] uncompress(byte[] buffer, int start, int[] length){
	    int inflated_end=0;

	    stream.next_in=buffer;
	    stream.next_in_index=start;
	    stream.avail_in=length[0];

	    while(true){
	      stream.next_out=tmpbuf;
	      stream.next_out_index=0;
	      stream.avail_out=BUF_SIZE;
	      int status=stream.inflate(JZlib.Z_PARTIAL_FLUSH);
	      switch(status){
	        case JZlib.Z_OK:
		  if(inflated_buf.length<inflated_end+BUF_SIZE-stream.avail_out){
	            int len=inflated_buf.length*2;
	            if(len<inflated_end+BUF_SIZE-stream.avail_out)
	              len=inflated_end+BUF_SIZE-stream.avail_out;
	            byte[] foo=new byte[len];
		    System.arraycopy(inflated_buf, 0, foo, 0, inflated_end);
		    inflated_buf=foo;
		  }
		  System.arraycopy(tmpbuf, 0,
				   inflated_buf, inflated_end,
				   BUF_SIZE-stream.avail_out);
		  inflated_end+=(BUF_SIZE-stream.avail_out);
	          length[0]=inflated_end;
		  break;
	        case JZlib.Z_BUF_ERROR:
	          if(inflated_end>buffer.length-start){
	            byte[] foo=new byte[inflated_end+start];
	            System.arraycopy(buffer, 0, foo, 0, start);
	            System.arraycopy(inflated_buf, 0, foo, start, inflated_end);
		    buffer=foo;
		  }
		  else{
	            System.arraycopy(inflated_buf, 0, buffer, start, inflated_end);
		  }
	          length[0]=inflated_end;
		  return buffer;
		default:
		  System.err.println("uncompress: inflate returnd "+status);
	          return null;
	      }
	    }
	  }
	  
	  
	  public static void main(String[] args) {
		  Compression c =  new CompressionImpl(10);
		  c.init(Compression.INFLATER, 6);
		  c.compress(new byte[]{(byte)5, (byte)0}, 0, new int[]{1});
    }
}
