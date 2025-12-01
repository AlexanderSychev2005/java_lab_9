package org.example.task1.task2;

import java.util.concurrent.atomic.AtomicInteger;

class RingBuffer<T> {
    private final T[] buffer;
    private int head = 0; // Read index
    private int tail = 0; // Write index
    private int count = 0; // Number of items in buffer
    private final int capacity;

    @SuppressWarnings("unchecked")
    public RingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = (T[]) new Object[capacity];
    }

    // Producer method: Adds an item
    public synchronized void push(T item) throws InterruptedException {
        // If buffer is full, wait until space is available.
        // Using 'while' instead of 'if' to prevent spurious wakeups.
        while (count == capacity) {
            this.wait();
        }
        buffer[tail] = item;
        tail = (tail + 1) % capacity; // Circular increment of the tail index, e.g. (9+1) % 10 = 0, return to the start
        count++;

        // Notify waiting threads (e.g., consumers waiting to pop)
        this.notifyAll();
    }
    // Consumer method: Removes an item
    public synchronized T pop() throws InterruptedException {
        // If buffer is empty, wait until data is available.
        while (count == 0) {
            this.wait();
        }
        T item = buffer[head];
        head = (head + 1) % capacity; // Circular increment of the head index
        count--;
        this.notifyAll(); // Notify waiting threads (e.g., producers waiting to push)
        return item;
    }
}

public class Task2RingBuffer {

    // Atomic counter for unique message IDs
    private static final AtomicInteger msgId = new AtomicInteger(1);

    public static void main(String[] args) {
        // Create two buffers for the pipeline
        RingBuffer<String> buffer1 = new RingBuffer<>(10);
        RingBuffer<String> buffer2 = new RingBuffer<>(10);

        // Streams - generators, start 5 Producer threads
        for (int i = 1; i <= 5; i++) {
            final int threadNum = i;
            Thread generator = new Thread(() -> {
                try {
                    while (true) {
                        String msg = "Потік №" + threadNum + " згенерував повідомлення " + msgId.getAndIncrement();
                        buffer1.push(msg);
                        Thread.sleep(100); // Simulate work
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            generator.setDaemon(true);
            generator.start();
        }

        // Streams - translators, translator (Middleman) threads
        for (int i = 1; i <= 2; i++) {
            final int threadNum = i;
            Thread translator = new Thread(() -> {
                try {
                    while (true) {
                        String rawMsg = buffer1.pop(); // Consume from buffer 1
                        String translatedMsg = "Потік №" + threadNum + " переклав повідомлення -> [" + rawMsg + "]"; // Transform message
                        buffer2.push(translatedMsg); // Produce to buffer 2
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            // Daemon threads will stop automatically when main thread finishes
            translator.setDaemon(true);
            translator.start();
        }

        // Main thread acts as the final Consumer
        System.out.println("Main thread started reading...");
        try {
            // Read exactly 100 messages
            for (int i = 1; i <= 100; i++) {
                String finalMsg = buffer2.pop();
                System.out.println("Main отримав (" + i + "/100): " + finalMsg);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Main thread finished.");
    }

    private static Thread getThread(int i, RingBuffer<String> buffer1) {
        final int threadNum = i;
        Thread generator = new Thread(() -> {
            try {
                while (true) {
                    String msg = "Потік №" + threadNum + " згенерував повідомлення " + msgId.getAndIncrement();
                    buffer1.push(msg);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        generator.setDaemon(true);
        return generator;
    }
}