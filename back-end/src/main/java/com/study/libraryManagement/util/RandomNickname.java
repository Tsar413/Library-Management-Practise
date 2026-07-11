package com.study.libraryManagement.util;

import java.util.Random;

public class RandomNickname {
    public static String generateRandomNickname(){
        Random random = new Random();
        int num = 100_000_000 + random.nextInt(900_000_000);
        return "User_" + num;
    }
}
