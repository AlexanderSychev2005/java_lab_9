package org.example.task1.task1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


class Account {
    private final int id;
    private int balance;
    // Each account has its own lock
    private final Lock lock = new ReentrantLock();

    public Account(int id, int initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }

    public int getId() {
        return id;
    }

    public int getBalance() {
        return balance;
    }

    public void withdraw(int amount) {
        balance -= amount;
    }

    public void deposit(int amount) {
        balance += amount;
    }

    public Lock getLock() {
        return lock;
    }
}

class Bank {
    private List<Account> accounts;

    public Bank(List<Account> accounts) {
        this.accounts = accounts;
    }

    // Method to transfer money between accounts thread-safely
    public void transfer(Account from, Account to, int amount) {
        if (from.getId() == to.getId()) return;

        // Avoiding Deadlock, Lock Ordering strategy: block in ascending order of ID
        // firstLock: the account with the lower ID
        Account firstLock = from.getId() < to.getId() ? from : to;
        Account secondLock = from.getId() < to.getId() ? to : from;

        // Lock 1 account
        firstLock.getLock().lock();
        try {
            // Lock 2 account
            secondLock.getLock().lock();
            try {
                // We can transfer money safely, since no other stream can access this two accounts
                if (from.getBalance() >= amount) {
                    from.withdraw(amount);
                    to.deposit(amount);
                }
            } finally {
                // Unlock 1 account
                secondLock.getLock().unlock();
            }
        } finally {
            // Unlock 2 account
            firstLock.getLock().unlock();
        }
    }

    // Calculate total money in the bank
    public long getTotalBalance() {
        long total = 0;
        for (Account acc : accounts) {
            acc.getLock().lock();
            try {
                total += acc.getBalance();
            } finally {
                acc.getLock().unlock();
            }
        }
        return total;
    }
}


public class Task1BankTest {
    public static void main(String[] args) throws InterruptedException {
        int numAccounts = 100;
        int initialBalance = 1000;
        int numThreads = 2000;

        // Accounts Creating
        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < numAccounts; i++) {
            accounts.add(new Account(i, initialBalance));
        }
        Bank bank = new Bank(accounts);

        // Initial total balance check
        long startTotal = bank.getTotalBalance();
        System.out.println("Початковий загальний баланс: " + startTotal);

        // Execute concurrent transfers, using the thread pool to manage threads efficiently
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numThreads);
        Random random = new Random();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    Account from = accounts.get(random.nextInt(numAccounts));
                    Account to = accounts.get(random.nextInt(numAccounts));
                    int amount = random.nextInt(100);
                    bank.transfer(from, to, amount);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        latch.await();
        executor.shutdown();

        // Final total balance check
        long endTotal = bank.getTotalBalance();
        System.out.println("Кінцевий загальний баланс:   " + endTotal);

        if (startTotal == endTotal) {
            System.out.println("Успішно перевели гроші! Сума грошей не змінилася.");
        } else {
            System.err.println("Виникла якась помилка! Баланс не збігається.");
        }
    }
}