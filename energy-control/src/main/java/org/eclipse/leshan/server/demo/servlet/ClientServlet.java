/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Orange - keep one JSON dependency
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;
import org.eclipse.leshan.core.link.lwm2m.attributes.DefaultLwM2mAttributeParser;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.codec.CodecException;
import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.DiscoverRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveCompositeRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadCompositeRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteAttributesRequest;
import org.eclipse.leshan.core.request.WriteCompositeRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.request.exception.ClientSleepingException;
import org.eclipse.leshan.core.request.exception.InvalidRequestException;
import org.eclipse.leshan.core.request.exception.InvalidResponseException;
import org.eclipse.leshan.core.request.exception.RequestCanceledException;
import org.eclipse.leshan.core.request.exception.RequestRejectedException;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.DiscoverResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveCompositeResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadCompositeResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteAttributesResponse;
import org.eclipse.leshan.core.response.WriteCompositeResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.californium.LeshanServer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonLinkSerializer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonLwM2mNodeDeserializer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonLwM2mNodeSerializer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonRegistrationSerializer;
import org.eclipse.leshan.server.demo.servlet.json.JacksonResponseSerializer;
import org.eclipse.leshan.server.registration.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.course.IoT;

/**
 * Service HTTP REST API calls.
 */
public class ClientServlet extends HttpServlet {

    private static final String FORMAT_PARAM = "format";
    private static final String TIMEOUT_PARAM = "timeout";
    private static final String REPLACE_PARAM = "replace";

    // for composite operation
    private static final String PATH_PARAM = "paths";
    private static final String PATH_FORMAT_PARAM = "pathformat";
    private static final String NODE_FORMAT_PARAM = "nodeformat";

    private static final Logger LOG = LoggerFactory.getLogger(ClientServlet.class);

    private static final long DEFAULT_TIMEOUT = 5000; // ms

    private static final long serialVersionUID = 1L;

    private final LeshanServer server;

    private final ObjectMapper mapper;
    private final LwM2mAttributeParser attributeParser;

    public ClientServlet(LeshanServer server) {
        this.server = server;
	//
	// 2IMN15: set server object and initialize state.
	//
	IoT.Initialize(server);
	
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addSerializer(Link.class, new JacksonLinkSerializer());
        module.addSerializer(Registration.class, new JacksonRegistrationSerializer(server.getPresenceService()));
        module.addSerializer(LwM2mResponse.class, new JacksonResponseSerializer());
        module.addSerializer(LwM2mNode.class, new JacksonLwM2mNodeSerializer());
        module.addDeserializer(LwM2mNode.class, new JacksonLwM2mNodeDeserializer());
        mapper.registerModule(module);
        attributeParser = new DefaultLwM2mAttributeParser();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // all registered clients
        if (req.getPathInfo() == null) {
            Collection<Registration> registrations = new ArrayList<>();
            for (Iterator<Registration> iterator = server.getRegistrationService().getAllRegistrations(); iterator
                    .hasNext();) {
                registrations.add(iterator.next());
            }

            String json = this.mapper.writeValueAsString(registrations.toArray(new Registration[] {}));
            resp.setContentType("application/json");
            resp.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String[] path = StringUtils.split(req.getPathInfo(), '/');
        if (path.length < 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }
        String clientEndpoint = path[0];

        // /endPoint : get client
        if (path.length == 1) {
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                resp.setContentType("application/json");
                resp.getOutputStream()
                        .write(this.mapper.writeValueAsString(registration).getBytes(StandardCharsets.UTF_8));
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
            }
            return;
        }
        // /composite : do Read-Composite request.
        if (path.length == 2 && "composite".equals(path[1])) {
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // get paths
                    String pathParam = req.getParameter(PATH_PARAM);
                    List<String> paths = Arrays.asList(pathParam.split(","));

                    // get content format
                    String pathContentFormatParam = req.getParameter(PATH_FORMAT_PARAM);
                    ContentFormat pathContentFormat = pathContentFormatParam != null
                            ? ContentFormat.fromName(pathContentFormatParam.toUpperCase())
                            : null;
                    String nodeContentFormatParam = req.getParameter(NODE_FORMAT_PARAM);
                    ContentFormat nodeContentFormat = nodeContentFormatParam != null
                            ? ContentFormat.fromName(nodeContentFormatParam.toUpperCase())
                            : null;

                    // create & process request
                    ReadCompositeRequest request = new ReadCompositeRequest(pathContentFormat, nodeContentFormat,
                            paths);
                    ReadCompositeResponse cResponse = server.send(registration, request, extractTimeout(req));
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }

        // /clients/endPoint/LWRequest/discover : do LightWeight M2M discover request on a given client.
        if (path.length >= 3 && "discover".equals(path[path.length - 1])) {
            String target = StringUtils.substringBetween(req.getPathInfo(), clientEndpoint, "/discover");
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // create & process request
                    DiscoverRequest request = new DiscoverRequest(target);
                    DiscoverResponse cResponse = server.send(registration, request, extractTimeout(req));
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }

        // /clients/endPoint/LWRequest : do LightWeight M2M read request on a given client.
        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                // get content format
                String contentFormatParam = req.getParameter(FORMAT_PARAM);
                ContentFormat contentFormat = contentFormatParam != null
                        ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                        : null;

                // create & process request
                ReadRequest request = new ReadRequest(contentFormat, target);
                ReadResponse cResponse = server.send(registration, request, extractTimeout(req));
                processDeviceResponse(req, resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (RuntimeException | InterruptedException e) {
            handleException(e, resp);
        }
    }

    private void handleException(Exception e, HttpServletResponse resp) throws IOException {
        if (e instanceof InvalidRequestException || e instanceof CodecException
                || e instanceof ClientSleepingException) {
            LOG.warn("Invalid request", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append("Invalid request:").append(e.getMessage()).flush();
        } else if (e instanceof RequestRejectedException) {
            LOG.warn("Request rejected", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Request rejected:").append(e.getMessage()).flush();
        } else if (e instanceof RequestCanceledException) {
            LOG.warn("Request cancelled", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Request cancelled:").append(e.getMessage()).flush();
        } else if (e instanceof InvalidResponseException) {
            LOG.warn("Invalid response", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Invalid Response:").append(e.getMessage()).flush();
        } else if (e instanceof InterruptedException) {
            LOG.warn("Thread Interrupted", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Thread Interrupted:").append(e.getMessage()).flush();
        } else {
            LOG.warn("Unexpected exception", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Unexpected exception:").append(e.getMessage()).flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];
        // /clients/endPoint/composite : do LightWeight M2M WriteComposite request on a given client.
        if (path.length == 2 && "composite".equals(path[1])) {
            try {

                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // get content format
                    String nodeContentFormatParam = req.getParameter(NODE_FORMAT_PARAM);
                    ContentFormat nodeContentFormat = nodeContentFormatParam != null
                            ? ContentFormat.fromName(nodeContentFormatParam.toUpperCase())
                            : null;

                    // get node values
                    String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
                    Map<LwM2mPath, LwM2mNode> values = mapper.readValue(content,
                            new TypeReference<HashMap<LwM2mPath, LwM2mNode>>() {
                            });
                    // create & process request
                    WriteCompositeResponse cResponse = server.send(registration,
                            new WriteCompositeRequest(nodeContentFormat, values, null), extractTimeout(req));
                    processDeviceResponse(req, resp, cResponse);

                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }

        // at least /endpoint/objectId/instanceId
        if (path.length < 3) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }

        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                if (path.length >= 3 && "attributes".equals(path[path.length - 1])) {
                    // create & process request WriteAttributes request
                    target = StringUtils.removeEnd(target, path[path.length - 1]);
                    LwM2mAttributeSet attributes = new LwM2mAttributeSet(
                            attributeParser.parseQueryParams(req.getQueryString()));
                    WriteAttributesRequest request = new WriteAttributesRequest(target, attributes);
                    WriteAttributesResponse cResponse = server.send(registration, request, extractTimeout(req));
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    // get content format
                    String contentFormatParam = req.getParameter(FORMAT_PARAM);
                    ContentFormat contentFormat = contentFormatParam != null
                            ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                            : null;

                    // get replace parameter
                    String replaceParam = req.getParameter(REPLACE_PARAM);
                    boolean replace = true;
                    if (replaceParam != null)
                        replace = Boolean.valueOf(replaceParam);

                    // create & process request
                    LwM2mNode node = extractLwM2mNode(target, req, new LwM2mPath(target));
                    WriteRequest request = new WriteRequest(replace ? Mode.REPLACE : Mode.UPDATE, contentFormat, target,
                            node);
                    WriteResponse cResponse = server.send(registration, request, extractTimeout(req));
                    processDeviceResponse(req, resp, cResponse);
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (RuntimeException | InterruptedException | InvalidAttributeException e) {
            handleException(e, resp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];

        // /clients/endPoint/composite/observe : do LightWeight M2M Observe-Composite request on a given client.
        if (path.length == 3 && "composite".equals(path[1]) && "observe".equals(path[2])) {
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // get paths
                    String pathParam = req.getParameter(PATH_PARAM);
                    String[] paths = pathParam.split(",");

                    // get content format
                    String pathContentFormatParam = req.getParameter(PATH_FORMAT_PARAM);
                    ContentFormat pathContentFormat = pathContentFormatParam != null
                            ? ContentFormat.fromName(pathContentFormatParam.toUpperCase())
                            : null;
                    String nodeContentFormatParam = req.getParameter(NODE_FORMAT_PARAM);
                    ContentFormat nodeContentFormat = nodeContentFormatParam != null
                            ? ContentFormat.fromName(nodeContentFormatParam.toUpperCase())
                            : null;

                    // create & process request
                    ObserveCompositeRequest request = new ObserveCompositeRequest(pathContentFormat, nodeContentFormat,
                            paths);
                    ObserveCompositeResponse cResponse = server.send(registration, request, extractTimeout(req));
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }

        // /clients/endPoint/LWRequest/observe : do LightWeight M2M observe request on a given client.
        if (path.length >= 3 && "observe".equals(path[path.length - 1])) {
            try {
                String target = StringUtils.substringBetween(req.getPathInfo(), clientEndpoint, "/observe");
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // get content format
                    String contentFormatParam = req.getParameter(FORMAT_PARAM);
                    ContentFormat contentFormat = contentFormatParam != null
                            ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                            : null;

                    // create & process request
                    ObserveRequest request = new ObserveRequest(contentFormat, target);
                    ObserveResponse cResponse = server.send(registration, request, extractTimeout(req));
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }

        String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);

        // /clients/endPoint/LWRequest : do LightWeight M2M execute request on a given client.
        if (path.length == 4) {
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    String params = null;
                    if (req.getContentLength() > 0) {
                        params = IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
                    }
                    ExecuteRequest request = new ExecuteRequest(target, params);
                    ExecuteResponse cResponse = server.send(registration, request, extractTimeout(req));
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }

        // /clients/endPoint/LWRequest : do LightWeight M2M create request on a given client.
        if (2 <= path.length && path.length <= 3) {
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // get content format
                    String contentFormatParam = req.getParameter(FORMAT_PARAM);
                    ContentFormat contentFormat = contentFormatParam != null
                            ? ContentFormat.fromName(contentFormatParam.toUpperCase())
                            : null;

                    // create & process request
                    LwM2mNode node = extractLwM2mNode(target, req, new LwM2mPath(target));
                    if (node instanceof LwM2mObjectInstance) {
                        CreateRequest request;
                        if (node.getId() == LwM2mObjectInstance.UNDEFINED) {
                            request = new CreateRequest(contentFormat, target,
                                    ((LwM2mObjectInstance) node).getResources().values());
                        } else {
                            request = new CreateRequest(contentFormat, target, (LwM2mObjectInstance) node);
                        }

                        CreateResponse cResponse = server.send(registration, request, extractTimeout(req));
                        processDeviceResponse(req, resp, cResponse);
                    } else {
                        throw new IllegalArgumentException("payload must contain an object instance");
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException | InterruptedException e) {
                handleException(e, resp);
            }
            return;
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];

        // /clients/endPoint/composite/observe : do LightWeight M2M observe request on a given client.
        if (path.length == 3 && "composite".equals(path[1]) && "observe".equals(path[2])) {
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // get paths
                    String pathParam = req.getParameter(PATH_PARAM);
                    String[] paths = pathParam.split(",");

                    server.getObservationService().cancelCompositeObservations(registration, paths);
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
            return;
        }

        // /clients/endPoint/LWRequest/observe : cancel observation for the given resource.
        if (path.length >= 3 && "observe".equals(path[path.length - 1])) {
            try {
                String target = StringUtils.substringsBetween(req.getPathInfo(), clientEndpoint, "/observe")[0];
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    server.getObservationService().cancelObservations(registration, target);
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (RuntimeException e) {
                handleException(e, resp);
            }
            return;
        }

        // /clients/endPoint/LWRequest/ : delete instance
        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                DeleteRequest request = new DeleteRequest(target);
                DeleteResponse cResponse = server.send(registration, request, extractTimeout(req));
                processDeviceResponse(req, resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (RuntimeException | InterruptedException e) {
            handleException(e, resp);
        }
    }

    /*
    private static void ShowSystemState()
    {
	// Method to show the system state.
	// It is called when something changed.
    }

    private static void setDimLevels()
    {
	// Compute dim settings for all luminaires
	int newDimLevel = 0;
	if (maxPowerUsage == 0) {
	    // No luminaires.
	    newDimLevel = 100;
	} else {
	    newDimLevel = powerBudget*100 / maxPowerUsage;
	    if (newDimLevel > 100) newDimLevel = 100;
	}
	// If needed, update luminaires
	if (newDimLevel != currentDimLevel) {
	    // Adjust all luminaires.
	    for (Map.Entry<String,LuminaireState> entry : luminaireStates.entrySet()) {
		// entry.getKey()  is LwM2mEntrypoint.
		// entry.getValue() is LuminaireState.
		Registration regis = lwServer.getRegistrationService().getByEndpoint(entry.getKey());
		writeInteger(regis, 33001, 0, 30004, newDimLevel);
	    }
	}
	currentDimLevel = newDimLevel;
    }
    
    private static void setLuminaires(boolean on)
    {
	// Adjust all luminaires.
	for (Map.Entry<String,LuminaireState> entry : luminaireStates.entrySet()) {
	    // entry.getKey()  is LwM2mEntrypoint.
	    // entry.getValue() is LuminaireState.
	    Registration regis = lwServer.getRegistrationService().getByEndpoint(entry.getKey());
	    writeBoolean(regis, 33001, 0, 30000, on);
	}
    }
    
    private static int readInteger(Registration registration, int objectId, int instanceId, int resourceId)
    {
        try {
	    ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
                ReadResponse cResponse = lwServer.send(registration, request, 5000);
                if (cResponse.isSuccess()) {
                   String sValue = ((LwM2mResource)cResponse.getContent()).getValue().toString();
                   try {
                      int iValue = Integer.parseInt(((LwM2mResource)cResponse.getContent()).getValue().toString());
                      return iValue;
                   }
                   catch (Exception e) {
                   }
                   float fValue = Float.parseFloat(((LwM2mResource)cResponse.getContent()).getValue().toString());
                   return (int)fValue;
                } else {
                   return 0;
                }
        }
        catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("exception in readInteger");
                return 0;
        }
    }

    private static String readString(Registration registration, int objectId, int instanceId, int resourceId)
    {
        try {
	    ReadRequest request = new ReadRequest(objectId, instanceId, resourceId);
                ReadResponse cResponse = lwServer.send(registration, request, 1000);
                if (cResponse.isSuccess()) {
                   String value = ((LwM2mResource)cResponse.getContent()).getValue().toString();
                   return value;
                } else {
                   return "";
                }
        }
        catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.println("exception in readString");
                return "";
        }
    }

    private static void writeInteger(Registration registration, int objectId, int instanceId, int resourceId, int value)
    {
	try {
	    WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
	    WriteResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		System.out.println("writeInteger: Success");
	    } else {
		System.out.println("writeInteger: Failed, " + cResponse.toString());
	    }
	}
	catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("writeInteger: exception");
	}
    }

    private static void writeString(Registration registration, int objectId, int instanceId, int resourceId, String value)
    {
	try {
	    WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
	    WriteResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		System.out.println("writeString: Success");
	    } else {
		System.out.println("writeString: Failed" + cResponse.toString());
	    }
	}
	catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("writeString: exception");
	}
    }

    private static void writeBoolean(Registration registration, int objectId, int instanceId, int resourceId, boolean value)
    {
	try {
	    WriteRequest request = new WriteRequest(objectId, instanceId, resourceId, value);
	    WriteResponse cResponse = lwServer.send(registration, request, 1000);
	    if (cResponse.isSuccess()) {
		System.out.println("writeBoolean: Success");
	    } else {
		System.out.println("writeBoolean: Failed" + cResponse.toString());
	    }
	}
	catch (Exception e) {
	    System.out.println(e.getMessage());
	    System.out.println("writeBoolean: exception");
	}
    }

    public static void handleRegistration(Registration registration)
    {
        // Check which objects are available.
        Map<Integer,org.eclipse.leshan.core.LwM2m.Version> supportedObject = registration.getSupportedObject();
        // Objects 33000 (Presence Detector) and 33001 (Luminaire).
        int latitude=0;
        int longitude=0;
        if (supportedObject.get(33000) != null ||
            supportedObject.get(33001) != null) {
           // Either Presence Detector or Luminaire exist.
           // Retrieve location information.
           if (supportedObject.get(6) != null) {
                // Retrieve location.
                String latRes="/6/0/0";
                String longRes = "/6/0/1";
                latitude = readInteger(registration,6,0,0);
                longitude = readInteger(registration, 6,0,1);
           }
        } else {
           System.out.println("new registration does not contain Presence Detector or Luminaire.");
        }
        if (supportedObject.get(33000) != null) {
           System.out.println("Presence Detector");
           PresenceDetectorState pdState = new PresenceDetectorState();
           // A presence detector.
           // Retrieve statis fields
           pdState.latitude = latitude;
           pdState.longitude = longitude;
           // Instead of the code above, retrieve resourses individually
	   pdState.power = Boolean.valueOf(readString(registration,33000,0,30000));
	   pdState.presence = Boolean.valueOf(readString(registration, 33000,0,30001));

           presenceStates.put(registration.getEndpoint(), pdState);
           // Observe dynamic fields
           try {
             System.out.println(">>ObserveRequest created << ");
	     ObserveResponse pdResponse = lwServer.send(registration, new ObserveRequest(33000,0,30001), 3000);
             System.out.println(">>ObserveRequest sent << ");
             if (pdResponse == null) {
                System.out.println(">> NULL <<");
             }
           }
           catch (Exception e) {
                System.out.println("Something wrong with observing presence detector.");
           }
        }
        if (supportedObject.get(33001) != null) {
	    System.out.println("Luminaire");
           LuminaireState lmState = new LuminaireState();
           lmState.latitude = latitude;
           lmState.longitude = longitude;
           lmState.peakpower = 0;
           lmState.type = "LED";
           lmState.type = readString(registration, 33001,0,30002);
           lmState.peakpower = readInteger(registration, 33001,0,30003);
           lmState.dimlevel = readInteger(registration, 33001,0,30004);
           luminaireStates.put(registration.getEndpoint(), lmState);
	   maxPowerUsage += lmState.peakpower;
	   // TODO: since maxPowerUsage is increased, dim levels of all
	   //       luminaires might have to be adjusted.
	   setDimLevels();
           // Observe relevant luminaire information.
           try {
	       System.out.println(">>ObserveRequest created << ");
	       ObserveResponse coResponse = lwServer.send(registration, new ObserveRequest(33001, 0, 30000), 1000);
	       System.out.println(">>ObserveRequest sent << ");

	       if (coResponse == null) {
		   System.out.println(">>ObserveRequest null << ");
	       }
           }
           catch (Exception e) {
                System.out.println("Something wrong with observing luminaire power.");
           }
        }
        if (supportedObject.get(33002) != null) {
	    // Demand Response sets the power budget.
	    System.out.println("Demand Response found");

           powerBudget = readInteger(registration, 33002,0,30004);
	   System.out.println("Power budget is " + powerBudget);
           // Observe relevant luminaire information.
           try {
	       System.out.println(">>ObserveRequest created << ");
	       ObserveResponse coResponse = lwServer.send(registration, new ObserveRequest(33002, 0, 30005), 1000);
	       System.out.println(">>ObserveRequest sent << ");
	       if (coResponse == null) {
		   System.out.println(">>ObserveRequest null << ");
	       }
          }
           catch (Exception e) {
                System.out.println("Something wrong with observing demand response.");
           }
        }
        // Read current status (location latitude and longitude, counter, ..)
        // Create observations of status, counter license plate.
        // Update list of known objects.
        // Update webpage.
        // Update 8x8 LED matrix.
        ShowSystemState();
    }

    public static void handleDeregistration(Registration registration)
    {
        // Update list of known objects.
        // Update webpage
        // Update 8x8 LED matrix
        String rid = registration.getEndpoint();
        Boolean changed = false;
        if (luminaireStates.containsKey(rid)) {
	    // Substract luminaire peak value.
	    LuminaireState lmState = luminaireStates.get(rid);
	    maxPowerUsage -= lmState.peakpower; 
	    System.out.println("Luminaire removed. Max usage is " + maxPowerUsage + ", power budget is " + powerBudget);
	    luminaireStates.remove(rid);
	    changed = true;
        }
        if (presenceStates.containsKey(rid)) {
           presenceStates.remove(rid);
           changed = true;
        }
        if (changed) {
	    setDimLevels();
	    // Update webpage.
          ShowSystemState();
        }
    }

    public static void handleObserveResponse(SingleObservation observation, Registration registration, ObserveResponse response)
    {
        if (registration != null && observation != null && response != null) {
        // Check whether registration is known in list of known objects.
           String rid = registration.getEndpoint();
           String obsPath = observation.getPath().toString();
           Boolean changed = false;
	   System.out.println(">>ObserveResponse " + obsPath);
           if (luminaireStates.containsKey(rid)) {
              //
              LuminaireState lmState = luminaireStates.get(rid);
              if (obsPath.equals("/33001/0/30000")) {
		  // Luminaire turned on or off.
              }
           }
           if (presenceStates.containsKey(rid)) {
                PresenceDetectorState pdState = presenceStates.get(rid);
                if (obsPath.equals("/33000/0/30001")) {
                   String csValue = ((LwM2mResource)response.getContent()).getValue().toString();
                   try {
                      boolean ciValue = Boolean.valueOf(csValue);
                      if (ciValue != pdState.presence) {
                        pdState.presence = ciValue;
                        presenceStates.put(rid,pdState);
			// Use luminaireStates to adjust power.
			// When there are multiple luminaires, ...
			setLuminaires(ciValue);
                        changed = true;
                      }
                   }
                   catch (Exception e) {
                        System.out.println("Exception in reading presence detector:" + e.getMessage());
                   }
                }
                if (obsPath.equals("/33000/0/30000")) {
                   String powValue = ((LwM2mResource)response.getContent()).getValue().toString();
		   try {
		       boolean powBool = Boolean.parseBoolean(powValue);
		       if (powBool != pdState.power) {
			   pdState.power = powBool;
			   presenceStates.put(rid,pdState);
			   changed = true;
		       }
		   }
                   catch (Exception e) {
                        System.out.println("Exception in reading presence detector:" + e.getMessage());
                   }
                }
           }
	   if (obsPath.equals("/33002/0/30005")) {
	       String powValue = ((LwM2mResource)response.getContent()).getValue().toString();
	       try {
		   int newPower = Integer.parseInt(powValue);
		   if (newPower != powerBudget) {
		       powerBudget = newPower;
		       changed = true;
		       System.out.println("Power budget is " + powerBudget + ",  max usage is " + maxPowerUsage);
		       // Use luminaireStates to adjust all the dim levels.
		   }
	       }
	       catch (Exception e) {
		   System.out.println("Exception in reading demand response:" + e.getMessage());
	       }

	   }
        // Update status of parking lot (available, reserved, occupied)
        // Update webpage
        // Update 8x8 LED matrix
           if (changed) {
	       setDimLevels();
                ShowSystemState();
           }
        }
    }
    */
 
    private void processDeviceResponse(HttpServletRequest req, HttpServletResponse resp, LwM2mResponse cResponse)
            throws IOException {
        if (cResponse == null) {
            LOG.warn(String.format("Request %s%s timed out.", req.getServletPath(), req.getPathInfo()));
            resp.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
            resp.getWriter().append("Request timeout").flush();
        } else {
            String response = this.mapper.writeValueAsString(cResponse);
            resp.setContentType("application/json");
            resp.getOutputStream().write(response.getBytes());
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private LwM2mNode extractLwM2mNode(String target, HttpServletRequest req, LwM2mPath path) throws IOException {
        String contentType = StringUtils.substringBefore(req.getContentType(), ";");
        if ("application/json".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
            LwM2mNode node;
            try {
                node = mapper.readValue(content, LwM2mNode.class);
            } catch (JsonProcessingException e) {
                throw new InvalidRequestException(e, "unable to parse json to tlv:%s", e.getMessage());
            }
            return node;
        } else if ("text/plain".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
            int rscId = Integer.valueOf(target.substring(target.lastIndexOf("/") + 1));
            return LwM2mSingleResource.newStringResource(rscId, content);
        }
        throw new InvalidRequestException("content type %s not supported", req.getContentType());
    }

    private long extractTimeout(HttpServletRequest req) {
        // get content format
        String timeoutParam = req.getParameter(TIMEOUT_PARAM);
        long timeout;
        if (timeoutParam != null) {
            try {
                timeout = Long.parseLong(timeoutParam) * 1000;
            } catch (NumberFormatException e) {
                timeout = DEFAULT_TIMEOUT;
            }
        } else {
            timeout = DEFAULT_TIMEOUT;
        }
        return timeout;
    }
}
