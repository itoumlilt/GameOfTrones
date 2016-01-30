package com.toumlilt.gameoftrones;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

class DownloadSanitary extends AsyncTask<Void, String, ArrayList<Sanitary>> {

    private String URL_SANITARY = "http://parisdata.opendatasoft.com/api/records/1.0/search/?dataset=sanisettesparis2011&facet=info&facet=libelle";
    private int HTTP_ALL_OKAY = 200;

    protected ArrayList<Sanitary> doInBackground(Void... unused)
    {
        ArrayList<Sanitary> als = new ArrayList<>();
        try {
            URL url = new URL(this.URL_SANITARY);
            HttpURLConnection httpConn = null;
            httpConn = (HttpURLConnection) url.openConnection();

            if(httpConn.getResponseCode()!=this.HTTP_ALL_OKAY)
                throw new RuntimeException("Impossible to download sanitaries");

            InputStream in = httpConn.getInputStream();
            BufferedReader bis = new BufferedReader(new InputStreamReader(in));
            JSONObject myJson = new JSONObject(bis.readLine());
            JSONArray jsaRecords = myJson.optJSONArray("records");

            for (int i = 0; i < jsaRecords.length(); i++) {
                JSONObject jsoFields = ((JSONObject) jsaRecords.get(i)).getJSONObject("fields");
                JSONArray jsoGeom = jsoFields.getJSONArray("geom_x_y");

                if(jsoGeom!= null)
                {
                    Double longitude = (Double)jsoGeom.get(0);
                    Double latitude = (Double)jsoGeom.get(1);
                    als.add(new Sanitary(latitude, longitude));
                }
            }
        }
        catch (IOException|JSONException e) {
            e.printStackTrace();
        }
        return als;
    }
}