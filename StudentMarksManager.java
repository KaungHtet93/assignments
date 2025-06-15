package assignment;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class StudentMarksManager {

    static final String FILE_NAME = "student_marks.dat";

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int choice;

        do {
            System.out.println("\nStudent Marks Manager");
            System.out.println("======================");
            System.out.println("1. Add New Student");
            System.out.println("2. Show Students");
            System.out.println("3. Search Students");
            System.out.println("0. Exit");
            System.out.println("======================");
            System.out.print("Enter your choice: ");
            choice = in.nextInt();
            in.nextLine();

            switch (choice) {
                case 1:
                    addStudent(in);
                    break;
                case 2:
                    showStudents();
                    break;
                case 3:
                    System.out.print("Enter name to search: ");
                    String searchName = in.nextLine();
                    searchStudentsByName(searchName);
                    break;
                case 0:
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        } while (choice != 0);

        in.close();
    }

    static void addStudent(Scanner in) {
        String id, name;
        Map<String, Integer> subjectMarks = new HashMap<>();

        System.out.print("Enter student ID: ");
        id = in.nextLine();

        System.out.print("Enter student name: ");
        name = in.nextLine();

        int more;
        do {
            System.out.print("Enter subject name: ");
            String subject = in.nextLine();

            int marks;
            do {
                System.out.print("Enter marks (0-100): ");
                marks = in.nextInt();
                in.nextLine();
            } while (marks < 0 || marks > 100);

            subjectMarks.put(subject, marks);

            System.out.print("Add another subject? (0 to stop): ");
            more = in.nextInt();
            in.nextLine();
        } while (more != 0);

        Student s = new Student(id, name, subjectMarks);

        try (FileOutputStream fos = new FileOutputStream(FILE_NAME, true);
             ObjectOutputStream oos = getObjectOutputStream(fos)) {
            oos.writeObject(s);
            System.out.println("Student saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void showStudents() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            while (true) {
                Student s = (Student) ois.readObject();

                // Create async task for each student
                CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("\nStudent ID: ").append(s.getId());
                    sb.append("\nName: ").append(s.getName());
                    sb.append("\nSubjects & Marks:");

                    for (Map.Entry<String, Integer> entry : s.getSubjectMarks().entrySet()) {
                        sb.append("\n  - ").append(entry.getKey()).append(": ").append(entry.getValue());
                    }

                    int total = s.getTotalMarks();
                    double avg = s.getAverageMarks().orElse(0.0);

                    sb.append("\nTotal Marks: ").append(total);
                    sb.append("\nAverage Marks: ").append(String.format("%.2f", avg));
                    sb.append("\n------------------------------");

                    return sb.toString();
                }).thenAccept(System.out::println);

                futures.add(future);
            }
        } catch (EOFException e) {
            // Done reading students
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error reading students.");
        }

        // Wait for all student tasks to finish
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    // Avoids writing header multiple times
    private static ObjectOutputStream getObjectOutputStream(OutputStream out) throws IOException {
        File file = new File(FILE_NAME);
        if (file.length() == 0) {
            return new ObjectOutputStream(out);
        } else {
            return new AppendableObjectOutputStream(out);
        }
    }

    // Custom ObjectOutputStream to prevent writing header repeatedly
    static class AppendableObjectOutputStream extends ObjectOutputStream {
        public AppendableObjectOutputStream(OutputStream out) throws IOException {
            super(out);
        }

        @Override
        protected void writeStreamHeader() throws IOException {
            reset(); // Avoid writing a new header
        }
    }
    static void searchStudentsByName(String nameToSearch) {
        boolean found = false;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            while (true) {
                Student s = (Student) ois.readObject();
                if (s.getName().equalsIgnoreCase(nameToSearch)) {
                    System.out.println("Match Found:");
                    System.out.println(s);
                    found = true;
                }
            }
        } catch (EOFException e) {
            // end of file, nothing more to read
            if (!found) {
                System.out.println("No student found with name: " + nameToSearch);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error reading students.");
        }
    }
}
