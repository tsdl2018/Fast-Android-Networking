/*
 *    Copyright (C) 2016 Amit Shekhar
 *    Copyright (C) 2011 Android Open Source Project
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.androidnetworking;

import android.app.Application;
import android.test.ApplicationTestCase;

import com.androidnetworking.common.ANConstants;
import com.androidnetworking.common.ANRequest;
import com.androidnetworking.common.ANResponse;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;

import org.junit.Rule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static java.util.concurrent.TimeUnit.SECONDS;

public class GetApiTest extends ApplicationTestCase<Application> {

    @Rule
    public final MockWebServer server = new MockWebServer();

    public GetApiTest() {
        super(Application.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        createApplication();
    }

    public void testGetRequest() throws InterruptedException {

        server.enqueue(new MockResponse().setBody("getResponse"));

        final AtomicReference<String> responseRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        AndroidNetworking.get(server.url("/").toString())
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        responseRef.set(response);
                        latch.countDown();
                    }

                    @Override
                    public void onError(ANError anError) {
                        assertTrue(false);
                    }
                });

        assertTrue(latch.await(2, SECONDS));

        assertEquals("getResponse", responseRef.get());
    }

    public void testGetRequest404() throws InterruptedException {

        server.enqueue(new MockResponse().setResponseCode(404).setBody("getResponse"));

        final AtomicReference<String> errorDetailRef = new AtomicReference<>();
        final AtomicReference<String> errorBodyRef = new AtomicReference<>();
        final AtomicReference<Integer> errorCodeRef = new AtomicReference<>();
        final CountDownLatch latch = new CountDownLatch(1);

        AndroidNetworking.get(server.url("/").toString())
                .build()
                .getAsString(new StringRequestListener() {
                    @Override
                    public void onResponse(String response) {
                        assertTrue(false);
                    }

                    @Override
                    public void onError(ANError anError) {
                        errorBodyRef.set(anError.getErrorBody());
                        errorDetailRef.set(anError.getErrorDetail());
                        errorCodeRef.set(anError.getErrorCode());
                        latch.countDown();
                    }
                });

        assertTrue(latch.await(2, SECONDS));

        assertEquals(ANConstants.RESPONSE_FROM_SERVER_ERROR, errorDetailRef.get());

        assertEquals("getResponse", errorBodyRef.get());

        assertEquals(404, errorCodeRef.get().intValue());

    }

    @SuppressWarnings("unchecked")
    public void testSynchronousGetRequest() throws InterruptedException {

        server.enqueue(new MockResponse().setBody("getResponse"));

        ANRequest request = AndroidNetworking.get(server.url("/").toString()).build();

        ANResponse<String> response = request.executeForString();

        assertEquals("getResponse", response.getResult());
    }

    @SuppressWarnings("unchecked")
    public void testSynchronousGetRequest404() throws InterruptedException {

        server.enqueue(new MockResponse().setResponseCode(404).setBody("getResponse"));

        ANRequest request = AndroidNetworking.get(server.url("/").toString()).build();

        ANResponse<String> response = request.executeForString();

        ANError error = response.getError();

        assertEquals("getResponse", error.getErrorBody());

        assertEquals(ANConstants.RESPONSE_FROM_SERVER_ERROR, error.getErrorDetail());

        assertEquals(404, error.getErrorCode());
    }

}