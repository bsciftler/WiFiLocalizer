package edu.fiu.mpact.wifilocalizer;

import android.net.wifi.ScanResult;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import edu.fiu.mpact.wifilocalizer.Utils.EncTrainDistMatchPair;
import edu.fiu.mpact.wifilocalizer.Utils.EncTrainDistPair;
import edu.fiu.mpact.wifilocalizer.Utils.TrainDistPair;


public class LocalizationEuclideanDistance {

    protected boolean mIsReady = false; // true if setup() has been called
    protected LocalizationData mData = null; // data gathered on this phone
    protected LocalizationData mFileData = null; // other downloaded data
    private LocalizeActivity mLocAct; // store for access to marker drawing methods

    /**
     * Calls LocalizeActivity.drawMarkers on the three points with minimum euclidean distance.
     *
     * @param results access points seen at a localization moment
     */
    public void localize(List<ScanResult> results) {
        if (!isReadyToLocalize()) return;

        final long startTime = System.currentTimeMillis();
        final List<TrainDistPair> resultList = new ArrayList<>();

        // iterate through every training location
        // for every access point seen at that training location, note the MAC addresses in common with localization results
        // if the localization data has more than half AP in common with this training location, add distance to resultList
        // then, move on to the next training location
        for (LocalizationData.Location loc : mData.getLocations()) {
            final Deque<LocalizationData.AccessPoint> trainingAps = mData.getAccessPoints(loc);
            final Set<ScanResult> scanResultsInCommon = LocalizationData.getCommonBssids(
                    LocalizationData.getAccessPointBssids(trainingAps), results);

            double distance = 0;
            if (scanResultsInCommon.size() > results.size() / 2) {
                for (ScanResult result : scanResultsInCommon) {
                    for (LocalizationData.AccessPoint reading : trainingAps) {
                        // exactly zero or one reading will have the same MAC
                        if (reading.mBssid.equals(result.BSSID)) {
                            distance += Math.pow(result.level - reading.mRssi, 2);
                            break;
                        }
                    }
                }

                distance = distance / scanResultsInCommon.size();
                resultList.add(new TrainDistPair(loc, distance));
            }
        }

        final long elapsedTime = System.currentTimeMillis() - startTime;
        Log.i("localize", "took " + elapsedTime + " ms");
        mLocAct.drawMarkers(sortAndWeight(resultList));
    }

    /**
     * Calls LocalizeActivity.drawMarkers on the three points with minimum euclidean distance.
     * The difference between localize and localize2 is that localize2 does not filter the scanResults
     * There should be no difference in results, but this will run slower yet use less memory.
     *
     * @param results access points seen at a localization moment
     */
    public void localize2(List<ScanResult> results) {
        if (!isReadyToLocalize()) return;

        final long startTime = System.currentTimeMillis();
        final ArrayList<TrainDistPair> resultList = new ArrayList<>();

        // iterate through every training location
        // for every access point seen at that training location, note the *number of* MAC addresses in common with localization results
        // if the localization data has more than half AP in common with this training location, add distance to resultList
        // then, move on to the next training location
        for (LocalizationData.Location loc : mData.getLocations()) {
            final Deque<LocalizationData.AccessPoint> trainingAps = mData.getAccessPoints(loc);
            final int count = LocalizationData.getCommonBssids(
                    LocalizationData.getAccessPointBssids(trainingAps), results).size();

            double distance = 0;
            if (count > results.size() / 2) {
                for (ScanResult result : results) {
                    // exactly zero or one reading will have the same MAC
                    for (LocalizationData.AccessPoint reading : trainingAps) {
                        if (reading.mBssid.equals(result.BSSID)) {
                            distance += Math.pow(result.level - reading.mRssi, 2);
                            break;
                        }
                    }
                }
                distance = distance / count;
                resultList.add(new TrainDistPair(loc, distance));
            }
        }

        final long elapsedTime = System.currentTimeMillis() - startTime;
        Log.i("localize2", "took " + elapsedTime + " ms");
        mLocAct.drawMarkers(sortAndWeight(resultList));
    }

    /**
     * Identical algorithm to localize(), but uses the dataset marked as file data in setup().
     *
     * @param results access points seen at a localization moment
     */
    public void fileLocalize(List<ScanResult> results) {
        if (!isReadyToLocalize()) return;

        final long startTime = System.currentTimeMillis();
        final ArrayList<TrainDistPair> resultList = new ArrayList<>();

        for (LocalizationData.Location loc : mFileData.getLocations()) {
            final Deque<LocalizationData.AccessPoint> trainingAps = mFileData.getAccessPoints(loc);
            final Set<ScanResult> scanResultsInCommon = LocalizationData.getCommonBssids(
                    LocalizationData.getAccessPointBssids(trainingAps), results);

            double distance = 0;
            if (scanResultsInCommon.size() > results.size() / 2) {
                for (ScanResult result : scanResultsInCommon) {
                    for (LocalizationData.AccessPoint reading : trainingAps) {
                        if (reading.mBssid.equals(result.BSSID)) {
                            distance += Math.pow(result.level - reading.mRssi, 2);
                            break;
                        }
                    }
                }

                distance = distance / scanResultsInCommon.size();
                resultList.add(new TrainDistPair(loc, distance));
            }
        }

        final long elapsedTime = System.currentTimeMillis() - startTime;
        Log.i("fileLocalize", "took " + elapsedTime + " ms");
        mLocAct.drawMarkers(sortAndWeight(resultList));
    }

    /**
     * Operates on fileData, but this method is **NOT** identical to localize2(). It penalizes bssids
     * in the list of localization scan results that did not occur in the training.
     *
     * @param results access points seen at a localization moment
     */
    public void fileLocalize2(List<ScanResult> results) {
        if (!isReadyToLocalize()) return;

        final long startTime = System.currentTimeMillis();
        final ArrayList<TrainDistPair> resultList = new ArrayList<>();

        for (LocalizationData.Location loc : mFileData.getLocations()) {
            final Deque<LocalizationData.AccessPoint> trainingAps = mFileData.getAccessPoints(loc);
            final Set<String> bssids = LocalizationData.getAccessPointBssids(trainingAps);
            final int count = LocalizationData.getCommonBssids(
                    LocalizationData.getAccessPointBssids(trainingAps), results).size();

            double distance = 0;
            if (count > results.size() / 2) {
                for (ScanResult result : results) {
                    if (bssids.contains(result.BSSID)) {
                        for (LocalizationData.AccessPoint reading : trainingAps) {
                            if (reading.mBssid.equals(result.BSSID)) {
                                distance += Math.pow(result.level - reading.mRssi, 2);
                                break;
                            }
                        }
                    } else {
                        distance += Math.pow(result.level + 100, 2);
                    }
                }

                resultList.add(new TrainDistPair(loc, distance));
            }
        }

        final long elapsedTime = System.currentTimeMillis() - startTime;
        Log.i("fileLocalize2", "took " + elapsedTime + " ms");
        mLocAct.drawMarkers(sortAndWeight(resultList));
    }

   /* public void remoteLocalize(List<ScanResult> results, long mMapId) {
        final AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        final Gson gson = new Gson();
        ArrayList<LocalizationData.AccessPoint> resultAPVs = new ArrayList<>();
        for (ScanResult res : results)
            resultAPVs.add(new LocalizationData.AccessPoint(res.BSSID, res.level));
        String jsondata = gson.toJson(resultAPVs);

        params.put("mapId", mMapId);
        params.put("scanData", jsondata);

        final long starttime = System.currentTimeMillis();
        client.addHeader("Content-Type", "application/json");
        client.post("http://eic15.eng.fiu.edu:8080/wifiloc/localize/dolocalize", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                System.out.println(new String(bytes) + " " + i);
                ArrayList<TrainDistPair> resultList;
                try {
                    resultList = gson.fromJson(new String(bytes), new
                            TypeToken<ArrayList<TrainDistPair>>() {
                            }.getType());
                } catch (Exception e) {
                    Toast.makeText(mLocAct, e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
                System.out.println("runtime = " + (System.currentTimeMillis() - starttime) + " ms");
                mLocAct.drawMarkers(sortAndWeight(resultList));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
            }
        });
    }

    public void remoteLocalize2(List<ScanResult> results, long mMapId) throws
            IllegalStateException {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        final Gson gson = new Gson();
        ArrayList<LocalizationData.AccessPoint> resultAPVs = new ArrayList<>();
        for (ScanResult res : results) {
            resultAPVs.add(new LocalizationData.AccessPoint(res.BSSID, res.level));
        }
        String jsondata = gson.toJson(resultAPVs);

        params.put("mapId", mMapId);
        params.put("scanData", jsondata);

        final long starttime = System.currentTimeMillis();
        client.addHeader("Content-Type", "application/json");
        client.post("http://eic15.eng.fiu.edu:8080/wifiloc/localize/dolocalize3", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                System.out.println(new String(bytes) + " " + i);
                ArrayList<TrainDistPair> resultList;
                try {
                    resultList = gson.fromJson(new String(bytes), new
                            TypeToken<ArrayList<TrainDistPair>>() {
                            }.getType());
                } catch (Exception e) {
                    Toast.makeText(mLocAct, e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
                System.out.println("runtime = " + (System.currentTimeMillis() - starttime) + " ms");
                mLocAct.drawMarkers(sortAndWeight(resultList));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
            }
        });
    }

    public void remotePrivLocalize(List<ScanResult> results, long mMapId, final PrivateKey sk, PublicKey pk) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        final Gson gson = new Gson();

        final long starttime = System.currentTimeMillis();
        // local sums
        ArrayList<String> scanAPs = new ArrayList<>();
        ArrayList<BigInteger> sum2comp = new ArrayList<>();
        ArrayList<BigInteger> sum3comp = new ArrayList<>();
        for (ScanResult res : results) {
            scanAPs.add(res.BSSID);
            sum3comp.add(Paillier.encrypt(BigInteger.valueOf((long) Math.pow(res.level, 2)), pk));
            sum2comp.add(Paillier.encrypt(BigInteger.valueOf((long) res.level * 2), pk)); // -2*v
        }

        //BigInteger sum3c = Paillier.encrypt(BigInteger.valueOf(sum3),pk);
        params.put("mapId", mMapId);
        params.put("scanAPs", gson.toJson(scanAPs));
        params.put("sum2comp", gson.toJson(sum2comp));
        params.put("sum3comp", gson.toJson(sum3comp));
        params.put("publicKey", gson.toJson(pk));

        client.addHeader("Content-Type", "application/json");
        client.setResponseTimeout(30000);
        client.post("http://eic15.eng.fiu.edu:8080/wifiloc/localize/doprivlocalize", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                System.out.println(new String(bytes) + " " + i);
                ArrayList<EncTrainDistMatchPair> resultList;
                ArrayList<TrainDistPair> plainResultList = new ArrayList<>();
                try {
                    resultList = gson.fromJson(new String(bytes), new
                            TypeToken<ArrayList<EncTrainDistMatchPair>>() {
                            }.getType());
                    System.out.println(resultList.size());
                } catch (Exception e) {
                    Toast.makeText(mLocAct, e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                System.out.println("runtime = " + (System.currentTimeMillis() - starttime) + " ms");
                // decrypt
                for (EncTrainDistMatchPair res : resultList) {
                    plainResultList.add(new TrainDistPair(res.trainLocation, Paillier.decrypt(res
                            .dist, sk).doubleValue() / (double) res.matches));
                }

                System.out.println("runtime2 = " + (System.currentTimeMillis() - starttime) + " " +
                        "ms");

                // draw
                mLocAct.drawMarkers(sortAndWeight(plainResultList));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
            }
        });
    }

    public void remotePrivLocalize2(List<ScanResult> results, long mMapId, final PrivateKey sk,
                                    PublicKey pk) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        final Gson gson = new Gson();

        final long starttime = System.currentTimeMillis();
        ArrayList<String> scanAPs = new ArrayList<>();
        long sum3 = 0;
        ArrayList<BigInteger> sum2comp = new ArrayList<>();
        for (ScanResult res : results) {
            scanAPs.add(res.BSSID);
            sum3 += Math.pow(res.level, 2);   // positive
            sum2comp.add(Paillier.encrypt(BigInteger.valueOf((long) res.level * 2), pk)); // -2*v
            System.out.println("res.level * 2 = " + res.level * 2);
        }

        BigInteger sum3c = Paillier.encrypt(BigInteger.valueOf(sum3), pk);
        params.put("mapId", mMapId);
        params.put("scanAPs", gson.toJson(scanAPs));
        params.put("sum2comp", gson.toJson(sum2comp));
        params.put("sum3", sum3c);
        params.put("publicKey", gson.toJson(pk));

        client.addHeader("Content-Type", "application/json");
        client.setResponseTimeout(30000);
        client.post("http://eic15.eng.fiu.edu:8080/wifiloc/localize/doprivlocalize3", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                System.out.println(new String(bytes) + " " + i);
                ArrayList<EncTrainDistPair> resultList;
                ArrayList<TrainDistPair> plainResultList = new ArrayList<>();
                try {
                    resultList = gson.fromJson(new String(bytes), new
                            TypeToken<ArrayList<EncTrainDistPair>>() {
                            }.getType());
                    System.out.println(resultList.size());
                } catch (Exception e) {
                    Toast.makeText(mLocAct, e.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                System.out.println("runtime = " + (System.currentTimeMillis() - starttime) + " ms");
                // decrypt
                for (EncTrainDistPair res : resultList) {
                    plainResultList.add(new TrainDistPair(res.trainLocation, Paillier.decrypt(res
                            .dist, sk).doubleValue()));
                }

                System.out.println("runtime2 = " + (System.currentTimeMillis() - starttime) + " " +
                        "ms");
                // draw
                mLocAct.drawMarkers(sortAndWeight(plainResultList));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
            }
        });
    }*/

    /**
     * Indicates whether the data structures are setup to do a localization.
     *
     * @return true if setup() has been called at least once, else false
     */
    public boolean isReadyToLocalize() {
        return mIsReady;
    }

    /**
     * Sets up and caches data to use for localization
     * Must call this method before running any localization.
     *
     * @return true always
     */
    public boolean setup(LocalizationData data, LocalizationData fileData, LocalizeActivity locact) {
        mData = data;
        mLocAct = locact;
        mFileData = fileData;
        mIsReady = true;

        return true;
    }

    /**
     * Return three lowest distances TrainDistPairs in resultList.
     *
     * @param resultList list to weight
     * @return a 9-tuple of three (x,y) pairs and three corresponding weights
     */
    private float[] sortAndWeight(List<TrainDistPair> resultList) {
        if (resultList.isEmpty()) return new float[] {};

        // all of the magic happens in this compareTo
        Collections.sort(resultList);

        System.out.println("result0 = " + resultList.get(0).dist);
        System.out.println("result1 = " + resultList.get(1).dist);
        System.out.println("result2 = " + resultList.get(2).dist);

        double tot = resultList.get(0).dist + resultList.get(1).dist + resultList.get(2).dist;
        double w0 = (1 - (resultList.get(0).dist / tot)) / 2.0;
        double w1 = (1 - (resultList.get(1).dist / tot)) / 2.0;
        double w2 = (1 - (resultList.get(2).dist / tot)) / 2.0;
        Log.d("weight", "w0 = " + w0 + " w1 = " + w1 + " w2 = " + w2);

        return new float[] {
                resultList.get(0).trainLocation.mX, resultList.get(0).trainLocation.mY,
                resultList.get(1).trainLocation.mX, resultList.get(1).trainLocation.mY,
                resultList.get(2).trainLocation.mX, resultList.get(2).trainLocation.mY,
                (float) w0, (float) w1, (float) w2
        };
    }
}
