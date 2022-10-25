package com.nextlabs.rms.service;

import com.nextlabs.common.shared.JsonTag;
import com.nextlabs.common.shared.JsonWraper;
import com.nextlabs.rms.exception.TagException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.ProjectTag;
import com.nextlabs.rms.hibernate.model.Tag;
import com.nextlabs.rms.hibernate.model.Tenant;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

public final class TagService {

    private TagService() {
    }

    @SuppressWarnings("unchecked")
    public static List<JsonTag> getTenantTags(DbSession session, String tenantId, int tagType) {
        List<JsonTag> tagList = new ArrayList<>();
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.eq("tenant.id", tenantId));
        criteria.add(Restrictions.eq("type", tagType));
        criteria.addOrder(Order.asc("orderId"));
        List<Tag> tags = criteria.list();
        if (!tags.isEmpty()) {
            for (Tag tag : tags) {
                tagList.add(toJson(tag));
            }
        }
        return tagList;
    }

    public static void persistTenantTags(DbSession session, Tenant tenant, List<JsonTag> tags, int tagType)
            throws TagException {
        if (tags != null) {
            List<JsonTag> insertUpdateTagList = new ArrayList<>();
            List<String> deleteTagList = new ArrayList<>();
            try {
                List<String> tagNameList = getTenantTagNames(session, tenant.getId(), tagType);
                int orderId = getMaxOrderId(session, tenant.getId(), tagType);
                for (String name : tagNameList) {
                    deleteTagList.add(name.toUpperCase());
                }
                for (JsonTag jasonTag : tags) {
                    String tagName = jasonTag.getName().toUpperCase();
                    if (deleteTagList.contains(tagName)) {
                        insertUpdateTagList.add(jasonTag);
                        deleteTagList.remove(deleteTagList.indexOf(tagName));
                    } else {
                        jasonTag.setOrderId(++orderId);
                        insertUpdateTagList.add(jasonTag);
                    }
                }

                deleteTenantTag(session, deleteTagList, tenant, tagType);
                saveTenantTag(session, insertUpdateTagList, tenant);

            } catch (Exception e) {
                throw new TagException(e);
            }
        }
    }

    public static Tag getTenantTagDetails(DbSession session, String tenantid, String tagName, int tagType) {
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.eq("tenant.id", tenantid));
        criteria.add(Restrictions.eq("type", tagType));
        criteria.add(Restrictions.eq("name", tagName).ignoreCase());
        return (Tag)criteria.uniqueResult();
    }

    private static void saveTenantTag(DbSession session, List<JsonTag> jsonTagList, Tenant tenant) {
        if (!jsonTagList.isEmpty()) {
            session.beginTransaction();
            for (JsonTag jsonTag : jsonTagList) {
                Tag tag = getTenantTagDetails(session, tenant.getId(), jsonTag.getName(), jsonTag.getType());
                if (tag == null) {
                    tag = new Tag();
                    tag.setOrderId(jsonTag.getOrderId());
                    tag.setName(jsonTag.getName());
                    tag.setTenant(tenant);
                    tag.setType(jsonTag.getType());
                    tag.setCreationTime(new Date());
                }
                tag.setDescription(jsonTag.getDescription());
                tag.setLastModified(new Date());
                session.saveOrUpdate(tag);
            }
            session.commit();
        }
    }

    private static void deleteTenantTag(DbSession session, List<String> tagList, Tenant tenant, int tagType) {
        if (!tagList.isEmpty()) {
            session.beginTransaction();
            for (String tagName : tagList) {
                Tag tag = getTenantTagDetails(session, tenant.getId(), tagName, tagType);
                session.delete(tag);
            }
            session.commit();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> getTenantTagNames(DbSession session, String tenantId, int tagType) {
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.eq("tenant.id", tenantId));
        criteria.add(Restrictions.eq("type", tagType));
        criteria.setProjection(Projections.property("name"));
        criteria.addOrder(Order.asc("orderId"));
        return criteria.list();
    }

    private static JsonTag toJson(Tag tag) {
        JsonTag json = new JsonTag();
        json.setName(tag.getName());
        json.setOrderId(tag.getOrderId());
        json.setDescription(tag.getDescription());
        json.setId(tag.getId());
        json.setType(tag.getType());
        return json;
    }

    private static int getMaxOrderId(DbSession session, String tenantId, int tagType) {
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.eq("tenant.id", tenantId));
        criteria.add(Restrictions.eq("type", tagType));
        criteria.setProjection(Projections.max("orderId"));
        Integer orderId = (Integer)criteria.uniqueResult();
        if (orderId == null) {
            orderId = 0;
        }
        return orderId;
    }

    public static void persistProjectTags(DbSession session, List<JsonWraper> tagMapList, Project project)
            throws TagException {
        if (tagMapList != null) {
            Set<Integer> tagIds = convertToTagIds(tagMapList);
            List<ProjectTag> deleteTagList = new ArrayList<>();
            try {
                int projectId = project.getId();
                List<ProjectTag> projectTagList = getProjectTagIds(session, projectId);

                for (ProjectTag projectTag : projectTagList) {
                    if (tagIds.contains(projectTag.getTag().getId())) {
                        tagIds.remove(projectTag.getTag().getId());
                    } else {
                        deleteTagList.add(projectTag);
                    }
                }

                deleteProjectTags(session, deleteTagList);
                saveProjectTag(session, tagIds, project);

            } catch (Exception e) {
                throw new TagException(e);
            }
        }
    }

    public static List<JsonTag> getProjectTags(DbSession session, int projectId) {
        List<JsonTag> tagList = new ArrayList<>();
        List<ProjectTag> projectTags = getProjectTagIds(session, projectId);
        if (!projectTags.isEmpty()) {
            for (ProjectTag projectTag : projectTags) {
                tagList.add(toJson(projectTag.getTag()));
            }
        }
        return tagList;
    }

    public static Tag getTagDetails(DbSession session, int tagId) {
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.eq("id", tagId));
        return (Tag)criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    private static List<ProjectTag> getProjectTagIds(DbSession session, int projectId) {
        Criteria criteria = session.createCriteria(ProjectTag.class);
        criteria.add(Restrictions.eq("project.id", projectId));
        return criteria.list();

    }

    private static void saveProjectTag(DbSession session, Set<Integer> tagIds, Project project) {
        if (!tagIds.isEmpty()) {
            session.beginTransaction();
            for (Integer tagId : tagIds) {
                Tag tag = getTagDetails(session, tagId);
                ProjectTag projectTag = new ProjectTag();
                projectTag.setProject(project);
                projectTag.setTag(tag);
                session.save(projectTag);
            }
            session.commit();
        }
    }

    private static void deleteProjectTags(DbSession session, List<ProjectTag> projectTags) {
        if (!projectTags.isEmpty()) {
            session.beginTransaction();
            for (ProjectTag projectTag : projectTags) {
                session.delete(projectTag);
            }
            session.commit();
        }
    }

    public static Set<Integer> convertToTagIds(List<JsonWraper> tagMapList) {
        Set<Integer> tagList = new HashSet<>();
        if (tagMapList != null) {
            for (JsonWraper wraper : tagMapList) {
                Integer tagId = wraper.getAsObject(Integer.class);
                tagList.add(tagId);
            }
        }
        return tagList;

    }

    @SuppressWarnings("unchecked")
    public static boolean areTagsOwnedByTenant(DbSession session, String tenantId, Set<Integer> tagIds, int tagType) {
        Criteria criteria = session.createCriteria(Tag.class);
        criteria.add(Restrictions.eq("tenant.id", tenantId));
        criteria.add(Restrictions.in("id", tagIds));
        criteria.add(Restrictions.eq("type", tagType));
        List<Tag> tags = criteria.list();
        return tags.size() == tagIds.size();
    }

}
