package com.barefoot_weather;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class urllib {
    public static StringBuffer urlopen(String url) {
        try {
            URL u = new URL(url);
            StringBuffer sb = new StringBuffer();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(u.openStream()));
            String str;
            while((str = in.readLine())!=null) {
                sb.append(str);
            }
            in.close();
            return sb;
        } catch(MalformedURLException e) {
            e.printStackTrace();
        } catch(IOException e) {
            return new StringBuffer();
        }
        return null;
    }
}


public class MainActivity extends ActionBarActivity {

    WebView mWebView;
    WebView linkWebView;
    TextView tv;
    EditText current_temperature;
    Spinner spinner_surface;
    Spinner spinner_sensation;

    Dialog feedback_app;
    Dialog feedback_weather_sensation;

    String feedback_email;
    String feedback_app_message;

    int spinner_surface_position;
    int spinner_sensation_position;
    String temperature;

    String buffer_uri = "http://wb.rpisarev.org.ua:8087/";
    String location = "Novosibirsk";
    String degree = "C";
    String language = "ru";

    String [] sensation_desc_spinner = new String[10];
    String [] list_surfaces_spinner = new String[100];

    String [] hints = {"Ходите босиком! :-)",
                       "Обувайтесь реже! ^_^",
                       "Проведите день без обуви! (8"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        mWebView = (WebView) findViewById(R.id.webView);
        linkWebView = (WebView) findViewById(R.id.webView2);

        Get_weather gw = new Get_weather();
        gw.execute(buffer_uri + language + "/" + degree + "/" + location);

        // random hint
        Random rand = new Random();
        TextView tvh = (TextView) findViewById(R.id.textView);
        tvh.setText(hints[rand.nextInt(hints.length)]);
        // Dialog feedback application

        feedback_app = new Dialog(MainActivity.this);
        feedback_app.setTitle("Feedback application");
        feedback_app.setContentView(R.layout.feedback_application_dialog);
        final EditText email = (EditText)feedback_app.findViewById(R.id.editText);
        final EditText feedback = (EditText)feedback_app.findViewById(R.id.editText2);

        Button sendFeedback = (Button)feedback_app.findViewById(R.id.btnsendfeedback);
        sendFeedback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
                Matcher m = p.matcher(email.getText());
                if (feedback.getText().length() > 0 &&
                        feedback.getText().length() < 64000 && m.matches()) {
                    feedback_email = email.getText().toString();
                    feedback_app_message = feedback.getText().toString();
                    feedback_app.hide();
                    email.setText("");
                    feedback.setText("");
                    try {
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpPost httppost = new HttpPost(buffer_uri + "feedbackapp");
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                        nameValuePairs.add(new BasicNameValuePair("email", feedback_email));
                        nameValuePairs.add(new BasicNameValuePair("message", feedback_app_message));
                        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                        httpclient.execute(httppost);
                        //if (response.getStatusLine().getStatusCode() != 200) {
                           // Toast.makeText(getBaseContext(), response.getStatusLine().getStatusCode(), Toast.LENGTH_SHORT).show();
                        //}
                    } catch (ClientProtocolException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        final Button button_feedback = (Button) findViewById(R.id.btnfeedbckapp);
        button_feedback.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                feedback_app.show();
            }
        });

        // Dialog feedback weather

        feedback_weather_sensation = new Dialog(MainActivity.this);
        feedback_weather_sensation.setTitle("Feedback sensation weather");
        feedback_weather_sensation.setContentView(R.layout.feedback_weather);
        current_temperature = (EditText)feedback_weather_sensation.findViewById(R.id.editsen);
        spinner_surface = (Spinner) feedback_weather_sensation.findViewById(R.id.spnsurface);
        spinner_sensation = (Spinner) feedback_weather_sensation.findViewById(R.id.spnsensation);
        Button sendFeedbackWeather = (Button) feedback_weather_sensation.findViewById(R.id.btnsendsensation);
        sendFeedbackWeather.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Pattern p = Pattern.compile("[-]?[1-9]\\d*");
                Matcher m = p.matcher(current_temperature.getText().toString());
                if (m.matches()) {
                    String curr_t = current_temperature.getText().toString();
                    int input_temp = Integer.parseInt(curr_t);
                    if (input_temp < 50 && input_temp > -50) {
                        feedback_weather_sensation.hide();
                        current_temperature.setText(temperature);
                        spinner_surface.setSelection(0);
                        spinner_sensation.setSelection(5);
                        try {
                            HttpClient httpclient = new DefaultHttpClient();
                            HttpPost httppost = new HttpPost(buffer_uri + "feedbackweather");
                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                            nameValuePairs.add(new BasicNameValuePair("location", location));
                            nameValuePairs.add(new BasicNameValuePair("degree", degree));
                            nameValuePairs.add(new BasicNameValuePair("temperature", curr_t));
                            nameValuePairs.add(new BasicNameValuePair("surface_id", Integer.toString(spinner_surface_position)));
                            nameValuePairs.add(new BasicNameValuePair("sensation_id", Integer.toString(spinner_sensation_position)));
                            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                            httpclient.execute(httppost);

                        } catch (ClientProtocolException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        final Button button_feedback_weather = (Button) findViewById(R.id.btnfeedbcksensation);
        button_feedback_weather.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                feedback_weather_sensation.show();
            }
        });


        // Tabhost
        tv=(TextView)findViewById(R.id.textView1);
        tv.setMovementMethod(ScrollingMovementMethod.getInstance());
        TabHost tabs = (TabHost) findViewById(android.R.id.tabhost);

        tabs.setup();

        TabHost.TabSpec spec = tabs.newTabSpec("tag1");

        spec.setContent(R.id.tab1);
        spec.setIndicator("Погода");
        tabs.addTab(spec);

        spec = tabs.newTabSpec("tag2");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Новости");
        tabs.addTab(spec);

        spec = tabs.newTabSpec("tag3");
        spec.setContent(R.id.tab3);
        spec.setIndicator("Ссылки");
        tabs.addTab(spec);

        tabs.setCurrentTab(0);


        Timer myTimer = new Timer();
        //Handler uiHandler = new Handler();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Get_weather gw = new Get_weather();
                gw.execute(buffer_uri + language + "/" + degree + "/" + location);
            }

        }, 0L, 60L * 1000 * 30);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    private class Get_weather extends AsyncTask<String, Void, Map<String,String[]>> {

        @Override
        protected Map<String, String[]> doInBackground(String... params) {
            Map<String, String[]> list = new HashMap<String, String[]>();
            String[] fail = {"error"};
            if (params.length > 0) {
                String json_str = (urllib.urlopen(params[0])).toString();
                JSONTokener tokener = new JSONTokener(json_str);

                try {
                    JSONObject value = (JSONObject) tokener.nextValue();
                    String [] weather = {value.getString("weather")};
                    String [] links = {value.getString("links")};
                    String [] surfaces = {value.getString("surfaces")};
                    list.put("weather", weather);
                    list.put("links", links);
                    list.put("surfaces", surfaces);


                    JSONObject feedback_json = value.getJSONObject("feedback");
                    String [] current_temp = {feedback_json.getString("current_temp")};
                    int size_vote = 10;
                    JSONArray array_desc = feedback_json.getJSONArray("sensation_desc");
                    JSONArray array_surface = feedback_json.getJSONArray("list_surfaces");
                    int count_surface = array_surface.length();
                    String [] votes_sensation = new String[size_vote];
                    String [] sensation_desc = new String[size_vote];
                    String [] list_surfaces = new String[count_surface];
                    for(int i=0; i<size_vote; i++)
                        sensation_desc[i] =  array_desc.getString(i);
                    for(int i=0; i<count_surface; i++)
                        list_surfaces[i] = array_surface.getString(i);
                    list.put("votes_sensation", votes_sensation);
                    list.put("sensation_desc", sensation_desc);
                    list.put("list_surfaces", list_surfaces);
                    list.put("current_temp", current_temp);

                    return list;
                }
                catch (JSONException e){
                    list.put("weather", fail);
                    list.put("links", fail);
                    list.put("surfaces", fail);
                    return list;
                }
            }
            list.put("weather", fail);
            list.put("links", fail);
            list.put("surfaces", fail);
            return list;
        }

        @Override
        protected void onPostExecute(Map<String, String[]> result) {
            super.onPostExecute(result);
            mWebView.loadDataWithBaseURL(
                    null, result.get("weather")[0],
                    "text/html", "en_US", null);
            linkWebView.loadDataWithBaseURL(
                    null, result.get("links")[0],
                    "text/html", "en_US", null);
            tv.setText(result.get("surfaces")[0]);
            sensation_desc_spinner = result.get("sensation_desc");
            list_surfaces_spinner = result.get("list_surfaces");
            temperature = result.get("current_temp")[0];
            current_temperature.setText(temperature);
            ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(
                    com.barefoot_weather.MainActivity.this,
                    android.R.layout.simple_spinner_item,
                    sensation_desc_spinner);
            adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(
                    com.barefoot_weather.MainActivity.this,
                    android.R.layout.simple_spinner_item,
                    list_surfaces_spinner);
            adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            spinner_sensation.setAdapter(adapter1);
            spinner_surface.setAdapter(adapter2);
            spinner_surface.setSelection(0);
            spinner_sensation.setSelection(5);
            spinner_surface.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    spinner_surface_position = position;
                }
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
            spinner_sensation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    spinner_sensation_position = position;
                }
                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });
        }
    }
}

