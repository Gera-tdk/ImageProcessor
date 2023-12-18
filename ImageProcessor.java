package org.example;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Графическое приложение для шифрования и дешифрования изображений формата bmp.
 */
public class ImageProcessor extends JFrame {

    /**
     * Панель с оригинальным изображением.
     */
    private JPanel originalPanel;

    /**
     * Панель с зашифрованным изображением.
     */
    private JPanel encryptedPanel;

    /**
     * Объект для шифрования изображений.
     */
    private ImageEncryptor encryptor;

    /**
     * Объект для дешифрования изображений.
     */
    private ImageDecryptor decryptor;

    /**
     * Конструктор класса ImageProcessor.
     * Инициализирует графический интерфейс и объекты для шифрования/дешифрования.
     */
    public ImageProcessor() {
        setTitle("Image Encryptor/Decryptor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        JButton encryptButton = new JButton("Encrypt");
        JButton decryptButton = new JButton("Decrypt");

        originalPanel = new JPanel();
        encryptedPanel = new JPanel();

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(encryptButton);
        buttonPanel.add(decryptButton);

        add(buttonPanel, BorderLayout.NORTH);
        add(originalPanel, BorderLayout.WEST);
        add(encryptedPanel, BorderLayout.EAST);

        // добавление слушателей для кнопок шифрования и дешифрования
        encryptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                encryptImage();
            }
        });

        decryptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                decryptImage();
            }
        });

        // Инициализация объектов ImageEncryptor и ImageDecryptor
        encryptor = new ImageEncryptor();
        decryptor = new ImageDecryptor();
    }

    /**
     * Шифрует изображение с использованием внедрения текста в младшие биты цветовых каналов.
     */
    private void encryptImage() {
        // запрос пути к оригинальному изображению и тексту для внедрения
        String imagePath = JOptionPane.showInputDialog("Enter the path to the original image:");
        String textToEmbed = JOptionPane.showInputDialog("Enter the text to embed:");

        try {
            // чтение оригинального изображения
            BufferedImage originalImage = ImageIO.read(new File(imagePath));
            // шифрование изображения
            BufferedImage encryptedImage = encryptor.encryptLSB(originalImage, textToEmbed);

            // сохранение зашифрованного изображения с новым именем
            String encryptedImagePath = addSuffixToFileName(imagePath, "_encrypted");
            ImageIO.write(encryptedImage, "bmp", new File(encryptedImagePath));

            // сообщение об успешном внедрении текста
            JOptionPane.showMessageDialog(this, "The text has been successfully embedded in the image.");
            System.exit(0);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Дешифрует изображение, восстанавливая текст из младших бит цветовых каналов.
     */
    private void decryptImage() {
        // запрос пути к зашифрованному изображению
        String imagePath = JOptionPane.showInputDialog("Enter the path to the encrypted image:");

        try {
            // чтение зашифрованного изображения
            BufferedImage encryptedImage = ImageIO.read(new File(imagePath));
            // дешифрование изображения и восстановление текста
            String decryptedText = decryptor.decryptLSB(encryptedImage);

            // отображение оригинального изображения
            ImageIcon originalImageIcon = new ImageIcon(encryptedImage);
            JLabel originalImageLabel = new JLabel(originalImageIcon);
            originalPanel.removeAll();
            originalPanel.add(originalImageLabel);
            originalPanel.revalidate();
            originalPanel.repaint();

            // отображение дешифрованного текста
            JTextArea textArea = new JTextArea(decryptedText);
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);


            // отображение измененного изображения с изменениями LSB (НЗБ)
            BufferedImage modifiedImage = encryptor.generateModifiedImage(encryptedImage);
            ImageIcon modifiedImageIcon = new ImageIcon(modifiedImage);
            JLabel modifiedImageLabel = new JLabel(modifiedImageIcon);
            encryptedPanel.removeAll();
            encryptedPanel.setLayout(new BorderLayout());
            encryptedPanel.add(scrollPane, BorderLayout.CENTER);
            encryptedPanel.add(modifiedImageLabel, BorderLayout.EAST);
            encryptedPanel.revalidate();
            encryptedPanel.repaint();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Добавляет суффикс к имени файла.
     *
     * @param filePath Исходный путь к файлу.
     * @param suffix   Суффикс для добавления.
     * @return Новый путь к файлу с добавленным суффиксом.
     */
    private String addSuffixToFileName(String filePath, String suffix) {
        int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex == -1) {
            return filePath + suffix;
        } else {
            return filePath.substring(0, dotIndex) + suffix + filePath.substring(dotIndex);
        }
    }

    /**
     * Точка входа в приложение. Создает и отображает графический интерфейс.
     *
     * @param args Аргументы командной строки (не используются).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageProcessor().setVisible(true));
    }
}

/**
 * Класс для шифрования изображений с использованием внедрения текста в младшие биты цветовых каналов.
 */
class ImageEncryptor {

    /**
     * Шифрует изображение путем внедрения текста в младшие биты цветовых каналов.
     *
     * @param originalImage Исходное изображение.
     * @param textToEmbed   Текст, который необходимо внедрить.
     * @return Зашифрованное изображение.
     */
    protected BufferedImage encryptLSB(BufferedImage originalImage, String textToEmbed) {
        BufferedImage encryptedImage = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        int textIndex = 0;
        for (int y = 0; y < originalImage.getHeight(); y++) {
            for (int x = 0; x < originalImage.getWidth(); x++) {
                int rgb = originalImage.getRGB(x, y);

                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                if (textIndex < textToEmbed.length()) {
                    char currentChar = textToEmbed.charAt(textIndex++);
                    int charValue = (int) currentChar;

                    // внедрение битов символа в цветовые значения RGB
                    red = (red & 0xFE) | ((charValue >> 7) & 0x1);
                    green = (green & 0xFE) | ((charValue >> 6) & 0x1);
                    blue = (blue & 0xFE) | ((charValue >> 5) & 0x1);
                }

                int encryptedRGB = (red << 16) | (green << 8) | blue;
                encryptedImage.setRGB(x, y, encryptedRGB);
            }
        }

        return encryptedImage;
    }

    /**
     * Генерирует измененное изображение с видимыми изменениями младших бит.
     *
     * @param encryptedImage Зашифрованное изображение.
     * @return Измененное изображение с видимыми изменениями LSB.
     */
    protected BufferedImage generateModifiedImage(BufferedImage encryptedImage) {
        BufferedImage modifiedImage = new BufferedImage(
                encryptedImage.getWidth(), encryptedImage.getHeight(), BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < encryptedImage.getHeight(); y++) {
            for (int x = 0; x < encryptedImage.getWidth(); x++) {
                int encryptedRGB = encryptedImage.getRGB(x, y);

                // извлечение младшего бита из каждого цветового канала
                int red = (encryptedRGB >> 16) & 0xFF;
                int green = (encryptedRGB >> 8) & 0xFF;
                int blue = encryptedRGB & 0xFF;

                // установка только младших битов в измененном изображении, делая их видимыми
                int modifiedRGB = (red & 0x1) | ((green & 0x1) << 7) | ((blue & 0x1) << 15);
                modifiedImage.setRGB(x, y, modifiedRGB);
            }
        }

        return modifiedImage;
    }
}

/**
 * Класс для дешифрования изображений и восстановления текста из младших бит цветовых каналов.
 */
class ImageDecryptor {

    /**
     * Дешифрует изображение и восстанавливает текст из младших бит цветовых каналов.
     *
     * @param encryptedImage Зашифрованное изображение.
     * @return Дешифрованный текст.
     */
    protected String decryptLSB(BufferedImage encryptedImage) {
        StringBuilder decryptedText = new StringBuilder();

        for (int y = 0; y < encryptedImage.getHeight(); y++) {
            for (int x = 0; x < encryptedImage.getWidth(); x++) {
                int rgb = encryptedImage.getRGB(x, y);

                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // извлечение младшего бита из каждого цветового канала
                char charValue = (char) ((red & 0x1) << 7 | (green & 0x1) << 6 | (blue & 0x1) << 5);
                decryptedText.append(charValue);
            }
        }

        return decryptedText.toString().trim();
    }
}