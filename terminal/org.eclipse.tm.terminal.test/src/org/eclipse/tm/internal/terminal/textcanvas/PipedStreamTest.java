package org.eclipse.tm.internal.terminal.textcanvas;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


class PipedStreamTest {
	static class ReadThread extends Thread implements Runnable {

		InputStream pi = null;

		OutputStream po = null;

		ReadThread(String process, InputStream pi, OutputStream po) {
			this.pi = pi;
			this.po = po;
			setDaemon(true);
		}

		public void run() {
			byte[] buffer = new byte[2048];
			int bytes_read;
			try {
				for (;;) {
					bytes_read = pi.read(buffer);
					if (bytes_read == -1) {
						po.close();
						pi.close();
						return;
					}
					po.write(buffer, 0, bytes_read);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
	static class FakeInputStream extends InputStream {
		public int read(byte[] b, int off, int len) throws IOException {
			if(N==0)
				return -1;
			int n=Math.min(len,N);
			N-=n;
			for (int i = off; i < off+n; i++) {
				b[i]='x';
			}
			return n;
		}
		int N;
		FakeInputStream(int n) {
			N=n;
		}
		public int read() throws IOException {
			throw new UnsupportedOperationException();
		}
		/* 
		 * available has to be implemented!
		 */
		public int available() throws IOException {
			return N;
		}
	}
	static class FakeOutputStream extends OutputStream {
		long N;
		long nTot;
		long t0=System.currentTimeMillis();

		public void write(int b) throws IOException {
			throw new UnsupportedOperationException();
		}
		public void write(byte[] b, int off, int len) throws IOException {
			N+=len;
			nTot+=len;
			if(N>1000*1000*10) {
				long t=System.currentTimeMillis();
				System.out.println(N/1024 + " kbyte in " +(t-t0)+" ms -> "+(N*1000)/((t-t0)*1024)+" kb/sec");
				t0=System.currentTimeMillis();
				N=0;
			}
		}
	}
	public static void main(String[] args) throws IOException, InterruptedException {
		runTest();
		runTest();
	}
	private static void runTest() throws IOException, InterruptedException {
		PipedInputStream writeIn = new PipedInputStream();
		PipedOutputStream readOut = new PipedOutputStream(writeIn);
		runPipe(writeIn, readOut);
	}
	public static void runPipe(InputStream writeIn, OutputStream readOut) throws InterruptedException {
		FakeInputStream in=new FakeInputStream(25*1000*1000);
		FakeOutputStream out=new FakeOutputStream();
		ReadThread rt = new ReadThread("reader", in , readOut);
		ReadThread wt = new ReadThread("writer", writeIn, out);
		long t0=System.currentTimeMillis();
		rt.start();
		wt.start();
		wt.join();
		long t=System.currentTimeMillis();
		long n=out.nTot;
		System.out.println(n + " byte in " +(t-t0)+" ms -> "+(1000*n)/((t-t0+1)*1024)+" kb/sec");
	}
}
