package com.nextlabs.rms.rs;

import com.nextlabs.common.shared.JsonResponse;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.common.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;

@Path("/")
public class ResourceListing {

    public ResourceListing() {
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String showAll(@Context Application application) {
        JsonResponse resp = new JsonResponse(true);
        for (Class<?> clazz : application.getClasses()) {
            if (clazz == ResourceListing.class) {
                continue;
            }

            if (isRestApi(clazz)) {
                Resource resource = Resource.builder(clazz).build();
                process(resp, "", resource);
            }
        }
        return resp.toJson();
    }

    private boolean isRestApi(Class<?> rc) {
        if (rc.isAnnotationPresent(Path.class)) {
            return true;
        }

        for (Class<?> i : rc.getInterfaces()) {
            if (i.isAnnotationPresent(Path.class)) {
                return true;
            }
        }
        return false;
    }

    private void process(JsonResponse resp, String uriPrefix, Resource resource) {
        String path = resource.getPath();
        String pathPrefix;
        if (StringUtils.hasText(path)) {
            if (path.startsWith("/") || uriPrefix.endsWith("/")) {
                pathPrefix = uriPrefix + path;
            } else {
                pathPrefix = uriPrefix + '/' + path;
            }
        } else {
            pathPrefix = uriPrefix;
        }

        List<Resource> resources = new ArrayList<Resource>();
        resources.addAll(resource.getChildResources());
        for (ResourceMethod method : resource.getAllMethods()) {
            if (ResourceMethod.JaxrsType.SUB_RESOURCE_LOCATOR.equals(method.getType())) {
                resources.add(Resource.from(resource.getResourceLocator().getInvocable().getDefinitionMethod().getReturnType()));
            } else {
                List<JsonWraper> list = new ArrayList<JsonWraper>();
                list.addAll(resp.getResultAsList(pathPrefix));
                list.add(new JsonWraper(new ApiInfo(method)));
                resp.putResult(pathPrefix, list);
            }
        }
        for (Resource childResource : resources) {
            process(resp, pathPrefix, childResource);
        }
    }

    public static final class ApiInfo {

        private String method;
        private String consumes;
        private String produces;
        private String function;
        private List<Param> params;

        public ApiInfo(ResourceMethod rm) {
            method = rm.getHttpMethod();
            function = rm.getInvocable().getDefinitionMethod().getName();
            consumes = toString(rm.getConsumedTypes());
            produces = toString(rm.getProducedTypes());
            List<Parameter> list = rm.getInvocable().getParameters();
            if (!list.isEmpty()) {
                params = new ArrayList<Param>(list.size());
                for (Parameter parameter : list) {
                    Param param = Param.toParam(parameter);
                    if (param != null) {
                        params.add(param);
                    }
                }
            }
        }

        public String getMethod() {
            return method;
        }

        public String getConsumes() {
            return consumes;
        }

        public String getProduces() {
            return produces;
        }

        public String getFunction() {
            return function;
        }

        public List<ResourceListing.Param> getParams() {
            return params;
        }

        private String toString(List<MediaType> list) {
            if (list.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(list.get(0).toString());
            for (int i = 1, size = list.size(); i < size; ++i) {
                sb.append(',').append(list.get(i).toString());
            }
            return sb.toString();
        }
    }

    public static final class Param {

        private String name;
        private String style;
        private String type;

        public Param(String name, String style, String type) {
            this.name = name;
            this.style = style;
            this.type = type;
        }

        public static Param toParam(Parameter parameter) {
            Annotation[] annotations = parameter.getAnnotations();
            if (annotations.length == 1) {
                if (annotations[0] instanceof QueryParam) {
                    String type = parameter.getType().toString();
                    String name = ((QueryParam)annotations[0]).value();
                    return new Param(name, "query", type);
                } else if (annotations[0] instanceof PathParam) {
                    String type = parameter.getType().toString();
                    String name = ((PathParam)annotations[0]).value();
                    return new Param(name, "path", type);
                } else if (annotations[0] instanceof HeaderParam) {
                    String type = parameter.getType().toString();
                    String name = ((HeaderParam)annotations[0]).value();
                    return new Param(name, "header", type);
                }
            }
            return null;
        }

        public String getName() {
            return name;
        }

        public String getStyle() {
            return style;
        }

        public String getType() {
            return type;
        }
    }
}
