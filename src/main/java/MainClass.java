import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class MainClass {


    public static void main(String[] args) throws IOException, InterruptedException {


        ForkJoinPool pool = new ForkJoinPool(8);
        FileCount fileCount = new FileCount(Paths.get("C:\\Users\\NoorZ\\Documents\\dir").toRealPath());
        Long current = System.currentTimeMillis();
        pool.invoke(fileCount);
        Long after = System.currentTimeMillis();
        System.out.println("time in millis = " + (after - current));
        for(int i=0;i<FileCount.lowerCountResult.length;i++){
            System.out.print((char)(i+'a') + "\t"+FileCount.lowerCountResult[i]+'\n');
        }



    }




}


class FileCount extends RecursiveAction {

    private Path dir;
    static BigInteger[] lowerCountResult =new BigInteger[26];
    private final static BlockingQueue<File> queue = new ArrayBlockingQueue<>(16);

    public FileCount(Path dir) {
        this.dir = dir;
        init(lowerCountResult);
    }

    final Runnable producer = ()->{
        final List<FileCount> walks = new ArrayList<>();
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.toFile().isDirectory()&&!dir.equals(FileCount.this.dir)) {
                        FileCount w = new FileCount(dir);
                        w.fork();
                        walks.add(w);
                        //Arrays.stream(dir.toFile().listFiles(f -> f.isFile()));

                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if(file.toFile().isFile()){
                        try {
                            queue.put(file.toFile());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return FileVisitResult.SKIP_SUBTREE;

                    }else{

                        return FileVisitResult.CONTINUE;

                    }
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
        for (FileCount w : walks) {
            w.join();
        }



    };

    final Runnable consumer = ()->{

        while(true){
            if(queue.isEmpty()){
                try {
                    Thread.sleep(200);
                    if(queue.isEmpty()){
                        break;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            else{
                try {
                    File file = queue.take();
                    process(file);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }



        }

    };

    private void process(File file) {

        BigInteger [] lowerCount = countLowerCase(file);
        incrementLetterCount(lowerCount);


    }


    public  BigInteger [] countLowerCase(File file)  {
        BigInteger  [] tempLetterCount = new BigInteger[26];
        init(tempLetterCount);
        try {
            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream fileReader = new BufferedInputStream(fin);
            int c;
            while ((c=fileReader.read() )!= -1){

                if (Character.isLowerCase((char)c) && c>='a'&&c<='z') {

                    try {
                        tempLetterCount[(char)c-'a']=tempLetterCount[(char)c-'a'].add(new BigInteger("1"));
                    } catch (Exception e) {

                        e.printStackTrace();
                        // System.out.println((char)c+ " --------------------- ");




                    }

                }
            }
            return tempLetterCount;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new BigInteger[26];



    }


    private  void init(BigInteger[] bigIntegers) {
        for (int i =0;i<bigIntegers.length;i++){
            bigIntegers[i] = new BigInteger("0");
        }

    }


    private   static synchronized void incrementLetterCount(BigInteger [] ... tempLetterCounts){

        for(BigInteger []temp:tempLetterCounts){
            for(int i=0;i<26;i++){
                lowerCountResult[i]=lowerCountResult[i].add(temp[i]);
            }
        }



    }


    /*public void start() throws InterruptedException {

        Thread producerThread = new Thread(producer);
        Thread[] consumerThreads = new Thread[8];
        for(int i=0;i<8;i++){
            consumerThreads[i] = new Thread(consumer);
        }


        producerThread.start();
        for(int i=0;i<8;i++){
            consumerThreads[i].start();
        }

        producerThread.join();
        for(int i=0;i<8;i++) {
            consumerThreads[i].join();
        }



    }*/

    @Override
    protected void compute() {
        Thread producerThread = new Thread(producer);
        Thread[] consumerThreads = new Thread[8];
        for(int i=0;i<8;i++){
            consumerThreads[i] = new Thread(consumer);
        }


        producerThread.start();
        for(int i=0;i<8;i++){
            consumerThreads[i].start();
        }

        try {
            producerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(int i=0;i<8;i++) {
            try {
                consumerThreads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}





