package com.deppon.spring;

public class a {

	public static volatile int num = 100;

	public static void main(String[] args) {
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					num++;
				}
			}

		}).start();

		new Thread(new Runnable() {

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println(num);
				}
			}

		}).start();
		;

	}
}
