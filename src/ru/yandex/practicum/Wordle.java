package ru.yandex.practicum;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Wordle {
    
    public static void main(String[] args) {
        PrintWriter logWriter = null;
        
        try {
            // Создаем лог-файл
            logWriter = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream("wordle.log"), StandardCharsets.UTF_8), true);
            
            logWriter.println("Запуск игры Wordle");
            
            // Загружаем словарь
            WordleDictionaryLoader loader = new WordleDictionaryLoader();
            WordleDictionary dictionary = loader.loadDictionary("russian_nouns.txt");
            logWriter.println("Словарь загружен, слов: " + dictionary.getWords().size());
            
            // Создаем игру
            WordleGame game = new WordleGame(dictionary);
            logWriter.println("Загадано слово: " + game.getAnswer());
            
            // Игровой цикл
            Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
            
            System.out.println("Добро пожаловать в Wordle!");
            System.out.println("У вас 6 попыток отгадать слово из 5 букв");
            System.out.println("Символы: + - правильная позиция, ^ - буква есть, но не на месте, - - буквы нет");
            System.out.println("Для подсказки нажмите Enter без ввода слова");
            
            while (!game.isGameOver()) {
                System.out.println("\nПопыток осталось: " + game.getRemainingSteps());
                System.out.print("> ");
                
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    // Подсказка
                    String hint = game.getHint();
                    if (hint != null) {
                        System.out.println("Подсказка: " + hint);
                        logWriter.println("Пользователь запросил подсказку: " + hint);
                    } else {
                        System.out.println("Подсказки недоступны");
                    }
                    continue;
                }
                
                try {
                    WordleGame.GameResult result = game.makeAttempt(input);
                    
                    System.out.println("> " + result.getWord());
                    System.out.println("> " + result.getAnalysis());
                    
                    if (result.isWin()) {
                        System.out.println("\n Поздравляем! Вы угадали слово!");
                        logWriter.println("Пользователь выиграл, слово: " + result.getWord());
                        break;
                    }
                    
                } catch (WordNotFoundInDictionaryException e) {
                    System.out.println("X " + e.getMessage());
                    logWriter.println("Пользователь ввел слово не из словаря: " + input);
                } catch (WordleGameException e) {
                    System.out.println("X " + e.getMessage());
                    logWriter.println("Ошибка игры: " + e.getMessage());
                }
            }
            
            if (!game.isWon()) {
                System.out.println("\n Игра окончена! Загаданное слово: " + game.getAnswer());
                logWriter.println("Пользователь проиграл, загаданное слово: " + game.getAnswer());
            }
            
            scanner.close();
            
        } catch (Exception e) {
            // Логируем все ошибки
            if (logWriter != null) {
                logWriter.println("Критическая ошибка: " + e.getMessage());
                e.printStackTrace(logWriter);
            } else {
                System.err.println("Критическая ошибка: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            if (logWriter != null) {
                logWriter.close();
            }
        }
    }
}
