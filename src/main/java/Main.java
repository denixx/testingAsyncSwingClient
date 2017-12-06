import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    public static final String url = "http://localhost:8080";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();
                frame.setTitle("Asynchronous requester");
                frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                MigLayout layout = new MigLayout();
                JPanel mainPanel = new JPanel(layout);
                frame.setContentPane(mainPanel);

                final JTextField resultText = new JTextField(30);
                JButton buttonStart = new JButton();

                mainPanel.add(buttonStart);
                mainPanel.add(resultText);

                buttonStart.setAction(new AbstractAction("Start") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        new MyWorker(url, resultText).execute();
                    }
                });

                frame.pack();
                frame.setVisible(true);
                buttonStart.requestFocusInWindow();
            }
        });
    }

    static class MyWorker extends SwingWorker<String, String> {
        private JTextField resultText;
        private HttpClient client;
        private String url;
        private BasicCookieStore cookies = new BasicCookieStore();


        public MyWorker(String rootUrl, JTextField resultText) {
            this.resultText = resultText;
            this.url = rootUrl;
            client = HttpClientBuilder.create().setDefaultCookieStore(cookies).build();
        }

        @Override
        protected String doInBackground() throws Exception {
            publish("Starting");
            HttpPost requestStart = new HttpPost(url+"/start");
            HttpResponse resultStart = client.execute(requestStart);
            publish("Got start response");
            if (resultStart.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String started = IOUtils.toString(resultStart.getEntity().getContent(), "UTF-8");
                publish("Got start response: \""+started+"\"");
                if ("started".equals(started)) {
                    publish("Got start response: \""+started+"\", proceeding");
                    String working = "-4";
                    do {
                        Thread.sleep(1000L);
                        HttpGet requestStatus = new HttpGet(url+"/status");
                        HttpResponse resultStatus = client.execute(requestStatus);
                        publish("Got status of job");
                        if (resultStatus.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            working = IOUtils.toString(resultStatus.getEntity().getContent(), "UTF-8");
                            publish("Got status of job: \""+working+"\"");
                        } else return "-3";
                    } while ("working".equals(working));
                    publish("Got status of job: \""+working+"\", job is done");
                    return working;
                } else return "-2";
            }
            return "-1";
        }

        @Override
        protected void process(List<String> chunks) {
            super.process(chunks);
            String s = chunks.get(chunks.size() - 1);
            resultText.setText(s);
        }

        @Override
        protected void done() {
            super.done();
            String s = "Working";
            try {
                s = get();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            } catch (ExecutionException e1) {
                e1.printStackTrace();
            }

            resultText.setText("Job result: " + s);
        }
    }
}
