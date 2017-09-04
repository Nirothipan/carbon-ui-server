/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.uis.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.uis.api.App;
import org.wso2.carbon.uis.api.exception.UISRuntimeException;
import org.wso2.carbon.uis.api.http.HttpRequest;
import org.wso2.carbon.uis.api.http.HttpResponse;
import org.wso2.carbon.uis.internal.deployment.AppRegistry;
import org.wso2.carbon.uis.internal.exception.DeploymentException;
import org.wso2.carbon.uis.internal.exception.HttpErrorException;
import org.wso2.carbon.uis.internal.io.StaticResolver;

import static org.wso2.carbon.uis.api.http.HttpResponse.CONTENT_TYPE_TEXT_HTML;
import static org.wso2.carbon.uis.api.http.HttpResponse.HEADER_CACHE_CONTROL;
import static org.wso2.carbon.uis.api.http.HttpResponse.HEADER_EXPIRES;
import static org.wso2.carbon.uis.api.http.HttpResponse.HEADER_PRAGMA;
import static org.wso2.carbon.uis.api.http.HttpResponse.HEADER_X_CONTENT_TYPE_OPTIONS;
import static org.wso2.carbon.uis.api.http.HttpResponse.HEADER_X_FRAME_OPTIONS;
import static org.wso2.carbon.uis.api.http.HttpResponse.HEADER_X_XSS_PROTECTION;
import static org.wso2.carbon.uis.api.http.HttpResponse.STATUS_BAD_REQUEST;
import static org.wso2.carbon.uis.api.http.HttpResponse.STATUS_INTERNAL_SERVER_ERROR;
import static org.wso2.carbon.uis.api.http.HttpResponse.STATUS_NOT_FOUND;
import static org.wso2.carbon.uis.api.http.HttpResponse.STATUS_OK;

public class RequestDispatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestDispatcher.class);

    private final StaticResolver staticResolver;

    public RequestDispatcher() {
        this(new StaticResolver());
    }

    public RequestDispatcher(StaticResolver staticResolver) {
        this.staticResolver = staticResolver;
    }

    public HttpResponse serve(HttpRequest request, AppRegistry appRegistry) {
        HttpResponse response = new HttpResponse();

        if (!request.isValid()) {
            serveDefaultErrorPage(STATUS_BAD_REQUEST, "Invalid URI '" + request.getUri() + "'.", response);
            return response;
        }
        if (request.isDefaultFaviconRequest()) {
            serveDefaultFavicon(request, response);
            return response;
        }

        App app;
        try {
            app = appRegistry.getApp(request.getContextPath());
        } catch (DeploymentException e) {
            String msg = "Cannot deploy an app for context path '" + request.getContextPath() + "'.";
            LOGGER.error(msg, e);
            serveDefaultErrorPage(STATUS_INTERNAL_SERVER_ERROR, msg, response);
            return response;
        }
        if (app == null) {
            serveDefaultErrorPage(STATUS_NOT_FOUND,
                                  "Cannot find an app for context path '" + request.getContextPath() + "'.", response);
            return response;
        }

        serve(app, request, response);
        return response;
    }

    private void serve(App app, HttpRequest request, HttpResponse response) {
        try {
            if (request.isStaticResourceRequest()) {
                staticResolver.serve(app, request, response);
            } else {
                servePage(app, request, response);
            }
        } catch (HttpErrorException e) {
            serveDefaultErrorPage(e.getHttpStatusCode(), e.getMessage(), response);
        } catch (UISRuntimeException e) {
            String msg = "A server error occurred while serving for request '" + request + "'.";
            LOGGER.error(msg, e);
            serveDefaultErrorPage(STATUS_INTERNAL_SERVER_ERROR, msg, response);
        } catch (Exception e) {
            String msg = "An unexpected error occurred while serving for request '" + request + "'.";
            LOGGER.error(msg, e);
            serveDefaultErrorPage(STATUS_INTERNAL_SERVER_ERROR, msg, response);
        }
    }

    private void servePage(App app, HttpRequest request, HttpResponse response) {
        try {
            setResponseSecurityHeaders(app, response);
            String html = app.renderPage(request);
            response.setContent(STATUS_OK, html, CONTENT_TYPE_TEXT_HTML);
        } catch (UISRuntimeException e) {
            throw e;
        } catch (Exception e) {
            // May be an UISRuntimeException cause this 'e' Exception. Let's unwrap 'e' and find out.
            Throwable th = e;
            while ((th = th.getCause()) != null) {
                if (th instanceof UISRuntimeException) {
                    // Cause of 'e' is an UISRuntimeException. Throw 'th' so that we can handle it properly.
                    throw (UISRuntimeException) th;
                }
            }
            // Cause of 'e' is not an UISRuntimeException.
            throw e;
        }
    }

    private void serveDefaultErrorPage(int httpStatusCode, String content, HttpResponse response) {
        response.setContent(httpStatusCode, content);
    }

    private void serveDefaultFavicon(HttpRequest request, HttpResponse response) {
        staticResolver.serveDefaultFavicon(request, response);
    }

    private void setResponseSecurityHeaders(App app, HttpResponse httpResponse) {
        httpResponse.setHeader(HEADER_X_CONTENT_TYPE_OPTIONS, "nosniff");
        httpResponse.setHeader(HEADER_X_XSS_PROTECTION, "1; mode=block");
        httpResponse.setHeader(HEADER_CACHE_CONTROL, "no-store, no-cache, must-revalidate, private");
        httpResponse.setHeader(HEADER_EXPIRES, "0");
        httpResponse.setHeader(HEADER_PRAGMA, "no-cache");
        httpResponse.setHeader(HEADER_X_FRAME_OPTIONS, "DENY");
    }
}