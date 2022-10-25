package com.nextlabs.rms.share;

import com.nextlabs.common.io.file.FileUtils;
import com.nextlabs.common.shared.Constants;
import com.nextlabs.common.shared.DeviceType;
import com.nextlabs.common.shared.JsonExpiry;
import com.nextlabs.common.shared.JsonSharedWithMeFile;
import com.nextlabs.common.shared.JsonSharedWithMeFileList;
import com.nextlabs.common.shared.JsonSharing;
import com.nextlabs.common.util.StringUtils;
import com.nextlabs.nxl.DecryptUtil;
import com.nextlabs.nxl.FilePolicy;
import com.nextlabs.nxl.NxlException;
import com.nextlabs.nxl.NxlFile;
import com.nextlabs.nxl.Rights;
import com.nextlabs.rms.auth.RMSUserPrincipal;
import com.nextlabs.rms.builder.OrderBuilder;
import com.nextlabs.rms.dto.repository.SharedFileDTO;
import com.nextlabs.rms.eval.CentralPoliciesEvaluationHandler;
import com.nextlabs.rms.eval.EvalResponse;
import com.nextlabs.rms.eval.EvaluationAdapterFactory;
import com.nextlabs.rms.eval.adhoc.AdhocEvalAdapter;
import com.nextlabs.rms.exception.FileAlreadyRevokedException;
import com.nextlabs.rms.exception.RMSException;
import com.nextlabs.rms.exception.ValidateException;
import com.nextlabs.rms.hibernate.DbSession;
import com.nextlabs.rms.hibernate.model.Project;
import com.nextlabs.rms.hibernate.model.ProjectSpaceItem;
import com.nextlabs.rms.hibernate.model.SharingRecipientKeyProject;
import com.nextlabs.rms.hibernate.model.SharingRecipientProject;
import com.nextlabs.rms.hibernate.model.SharingTransaction;
import com.nextlabs.rms.hibernate.model.User;
import com.nextlabs.rms.hibernate.model.UserSession;
import com.nextlabs.rms.locale.RMSMessageHandler;
import com.nextlabs.rms.manager.SharedFileManager;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryProject;
import com.nextlabs.rms.repository.defaultrepo.DefaultRepositoryTemplate;
import com.nextlabs.rms.repository.defaultrepo.InvalidDefaultRepositoryException;
import com.nextlabs.rms.repository.defaultrepo.StoreItem;
import com.nextlabs.rms.repository.defaultrepo.StoreItemManager;
import com.nextlabs.rms.repository.exception.RepositoryException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;
import com.nextlabs.rms.util.PolicyEvalUtil;
import com.nextlabs.rms.util.RepositoryFileUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

public class ShareProjectMapper implements IShareMapper {

    private static final String UPDATE_SHARING_RECIPIENTS_STATUS_FLAG = "UPDATE " + SharingRecipientProject.class.getName() + " s set s.status = 1 where s.id.duid = :duid";
    private static final String UPDATE_SHARING_TRANSACTION = "UPDATE " + SharingTransaction.class.getName() + " s set s.status = 1 where s.id in (select s2.id from " + SharingTransaction.class.getName() + " s2 join s2.projectNxl sp where sp.duid = :duid)";

    @Override
    public StoreItem getStoreItemByTransaction(String transactionId) {
        try (DbSession session = DbSession.newSession()) {
            SharingTransaction tx = session.get(SharingTransaction.class, transactionId);
            return getStoreItemByDuid(session, tx.getProjectNxl().getDuid());
        }
    }

    @Override
    public List<?> getRecipientList(String duid, Collection<JsonSharing.JsonRecipient> recipients) {
        List<Integer> projectIds = new ArrayList<>(recipients.size());
        recipients.forEach((recipient) -> projectIds.add(recipient.getProjectId()));
        try (DbSession session = DbSession.newSession()) {
            return getRecipientList(session, duid, projectIds);
        }
    }

    public List<SharingRecipientProject> getRecipientList(DbSession session, String duid,
        Collection<Integer> projectIds) {
        if (projectIds.isEmpty()) {
            return Collections.emptyList();
        }
        SharingRecipientKeyProject[] ids = new SharingRecipientKeyProject[projectIds.size()];
        int index = 0;
        for (Integer projectId : projectIds) {
            SharingRecipientKeyProject key = new SharingRecipientKeyProject();
            key.setDuid(duid);
            key.setProjectId(projectId);
            ids[index++] = key;
        }

        Criteria criteria = session.createCriteria(SharingRecipientProject.class);
        criteria.add(Restrictions.in("id", ids));
        criteria.add(Restrictions.ne("status", 1));
        return criteria.list();
    }

    @Override
    public String updateSharingTransaction(SharedFileDTO dto, int userId, String metadata)
            throws ValidateException, RMSException {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("duid", dto.getDocumentUID()));
            ProjectSpaceItem item = (ProjectSpaceItem)criteria.uniqueResult();
            if (item == null) {
                throw new ValidateException(400, "Could not find document in project.");
            } else {
                //                List<Integer> projectIds = Ints.asList(dto.getProjectId());
                //                if (item.getProject().getId() != dto.getProjectId() && getRecipientList(session, dto.getDocumentUID(), projectIds).isEmpty()) {
                //                    throw new ValidateException(403, "Access denied");
                //                }
                item.setLastModified(dto.getUpdatedDate());
                item.setStatus(ProjectSpaceItem.Status.SHARED);
                session.update(item);
            }

            SharingTransaction st = new SharingTransaction();
            st.setFromSpace(Constants.SHARESPACE.PROJECTSPACE);
            st.setProjectNxl(item);
            st.setUser(session.load(User.class, dto.getUserId()));
            st.setDeviceId(dto.getDeviceId());
            st.setDeviceType(DeviceType.valueOf(dto.getDeviceType()).ordinal());
            st.setCreationTime(dto.getCreatedDate());
            st.setComment(dto.getComment());
            st.setSourceProjectId(dto.getSourceProjectId());
            session.save(st);
            session.flush();
            session.refresh(st);
            String transactionId = st.getId();

            Set<Integer> projectIds = (Set<Integer>)dto.getShareWith();

            List<SharingRecipientProject> list = getRecipientList(session, st.getProjectNxl().getDuid(), projectIds);
            for (SharingRecipientProject recipient : list) {
                recipient.setTransaction(st);
                recipient.setLastModified(st.getCreationTime());
                projectIds.remove(recipient.getId().getProjectId());
            }

            for (Integer projectId : projectIds) {
                SharingRecipientProject recipient = new SharingRecipientProject();
                SharingRecipientKeyProject key = new SharingRecipientKeyProject();
                key.setDuid(st.getProjectNxl().getDuid());
                key.setProjectId(projectId);
                recipient.setId(key);
                recipient.setTransaction(st);
                recipient.setLastModified(st.getCreationTime());
                session.save(recipient);
            }
            session.commit();
            return transactionId;
        }
    }

    @Override
    public SharingTransaction getFirstTransactionByDuid(String duid) {
        try (DbSession session = DbSession.newSession()) {
            Criteria criteria = session.createCriteria(SharingTransaction.class);
            criteria.add(Restrictions.eq("projectNxl.duid", duid));
            criteria.add(Restrictions.eq("status", 0));
            criteria.addOrder(Order.asc("creationTime"));
            criteria.setFirstResult(0);
            criteria.setMaxResults(1);
            List<SharingTransaction> transactions = criteria.list();
            return transactions == null || transactions.isEmpty() ? null : transactions.get(0);
        }
    }

    @Override
    public StoreItem getStoreItemByDuid(String duid) {
        try (DbSession session = DbSession.newSession()) {
            return getStoreItemByDuid(session, duid);
        }
    }

    public StoreItem getStoreItemByDuid(DbSession session, String duid) {
        Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
        criteria.add(Restrictions.eq("duid", duid));
        ProjectSpaceItem metadata = (ProjectSpaceItem)criteria.uniqueResult();
        return StoreItemManager.projectItemToStoreItem(metadata);
    }

    @Override
    public Set<Integer> removeRecipients(List<?> recipients) {
        try (DbSession session = DbSession.newSession()) {
            List<SharingRecipientProject> list = (List<SharingRecipientProject>)recipients;
            Set<Integer> removed = new HashSet<>(list != null && !list.isEmpty() ? list.size() : 0);
            session.beginTransaction();
            for (SharingRecipientProject recipient : list) {
                removed.add(recipient.getId().getProjectId());
                session.delete(recipient);
            }
            session.commit();
            return removed;
        }
    }

    @Override
    public SharingTransaction getSharingTransaction(String transactionId) {
        return null;
    }

    private Criteria getSharedWithMeFilesListQuery(DbSession session, String searchString, Integer projectId) {
        DetachedCriteria dc = DetachedCriteria.forClass(SharingRecipientProject.class);
        dc.add(Restrictions.eq("id.projectId", projectId));
        dc.setProjection(Projections.property("transaction.id"));
        Criteria c = session.createCriteria(ProjectSpaceItem.class, "ps").createAlias("ps.transactions", "st", JoinType.INNER_JOIN).createAlias("st.user", "stu", JoinType.INNER_JOIN).createAlias("ps.project", "p", JoinType.INNER_JOIN);
        c.add(Subqueries.propertyIn("st.id", dc));
        c.add(Restrictions.eq("ps.status", ProjectSpaceItem.Status.SHARED));
        if (StringUtils.hasText(searchString)) {
            c.add(Restrictions.ilike("ps.filePathSearchSpace", searchString, MatchMode.ANYWHERE));
        }
        return c;
    }

    private long getTotalFiles(DbSession session, String searchString, Integer projectId) {
        Criteria c = getSharedWithMeFilesListQuery(session, searchString, projectId);
        c.setProjection(Projections.rowCount());
        Number total = (Number)c.uniqueResult();
        return total.longValue();
    }

    @Override
    public JsonSharedWithMeFileList getSharedWithMeFiles(Integer page, Integer size, String orderBy,
        String searchString, Object projectId) throws GeneralSecurityException {
        page = page != null && page.intValue() > 0 ? page : 1;
        size = size != null && size.intValue() > 0 ? size : -1;
        List<Order> orders = Collections.emptyList();
        if (StringUtils.hasText(orderBy)) {
            Map<String, String> supportedFields = new HashMap<>(4);
            supportedFields.put("sharedBy", "stu.email");
            supportedFields.put("sharedDate", "st.creationTime");
            supportedFields.put("name", "ps.filePathSearchSpace");
            supportedFields.put("size", "ps.size");
            OrderBuilder builder = new OrderBuilder(supportedFields);
            List<String> list = StringUtils.tokenize(orderBy, ",");
            for (String s : list) {
                builder.add(s);
            }
            orders = builder.build();
        }
        JsonSharedWithMeFileList result = new JsonSharedWithMeFileList();
        try (DbSession session = DbSession.newSession()) {
            List<JsonSharedWithMeFile> fileInfo = getSharedWithMeFileList(session, page, size, orders, searchString, (Integer)projectId);
            result.setFiles(fileInfo);
            result.setTotalFiles(getTotalFiles(session, searchString, (Integer)projectId));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<JsonSharedWithMeFile> getSharedWithMeFileList(DbSession session, Integer page, Integer size,
        List<Order> orders, String searchString, Integer projectId) throws GeneralSecurityException {
        Criteria c = getSharedWithMeFilesListQuery(session, searchString, projectId);
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                c.addOrder(order);
            }
        }
        if (size > 0) {
            c.setMaxResults(size);
            c.setFirstResult((page - 1) * size);
        }
        ProjectionList projectionList = Projections.projectionList();
        projectionList.add(Projections.property("ps.duid").as("duid"));
        projectionList.add(Projections.property("ps.filePathDisplay").as("filePathDisplay"));
        projectionList.add(Projections.property("ps.size").as("size"));
        projectionList.add(Projections.property("st.creationTime").as("sharedDate"));
        projectionList.add(Projections.property("stu.email").as("sharedBy"));
        projectionList.add(Projections.property("st.id").as("transactionId"));
        projectionList.add(Projections.property("ps.permissions").as("permissions"));
        projectionList.add(Projections.property("st.comment").as("comment"));
        projectionList.add(Projections.property("st.sourceProjectId").as("sharedByProject"));
        projectionList.add(Projections.property("p.name").as("sharedByProjectName"));
        c.setProjection(projectionList);
        List<Object[]> list = c.list();
        List<JsonSharedWithMeFile> result = new ArrayList<>(list.size());
        Map<Integer, String> projectIdNamesMap = list.isEmpty() ? null : getAllProjectNames(session, projectId);
        for (Object[] item : list) {
            JsonSharedWithMeFile file = new JsonSharedWithMeFile();
            file.setDuid((String)item[0]);
            file.setFileName(FileUtils.getName((String)item[1]));
            file.setFileType(RepositoryFileUtil.getOriginalFileExtension(file.getFileName()));
            file.setSize((Long)item[2]);
            Date sharedDate = (Date)item[3];
            if (sharedDate != null) {
                file.setSharedDate(sharedDate.getTime());
            }
            file.setSharedBy((String)item[4]);
            file.setTransactionId((String)item[5]);
            file.setTransactionCode(SharedFileManager.getTransactionCode((String)item[5]));
            int permissions = (int)item[6];
            Rights[] rights = Rights.fromInt(permissions);
            file.setRights(Rights.toStrings(rights));
            if (item[7] != null) {
                file.setComment(StringEscapeUtils.escapeHtml4((String)item[7]));
            }
            if (item[8] != null) {
                file.setSharedByProject(((Integer)item[8]).toString());
                file.setSharedByProjectName(projectIdNamesMap.get(Integer.valueOf(file.getSharedByProject())));
            }
            file.setProtectionType(permissions == 0 ? Constants.ProtectionType.CENTRAL.ordinal() : Constants.ProtectionType.ADHOC.ordinal());
            file.setOwner(false);
            result.add(file);
        }
        return result;
    }

    @Override
    public JsonSharedWithMeFile getSharedWithMeFile(String transactionId, RMSUserPrincipal principal,
        String recipientMembership)
            throws RMSException {
        try (DbSession session = DbSession.newSession()) {
            SharingTransaction st = session.get(SharingTransaction.class, transactionId);
            ProjectSpaceItem nxl = st.getProjectNxl();
            Rights[] rights = null;
            JsonExpiry validity = new JsonExpiry();
            DefaultRepositoryTemplate repository;
            byte[] downloadedFileBytes;
            Map<String, String[]> tags;
            repository = new DefaultRepositoryProject(session, principal, st.getProjectNxl().getProject().getId());
            downloadedFileBytes = RepositoryFileUtil.downloadPartialFileFromRepo(repository, nxl.getFilePath(), nxl.getFilePathDisplay());
            JsonSharedWithMeFile file = new JsonSharedWithMeFile();

            try (NxlFile metaData = NxlFile.parse(downloadedFileBytes)) {
                tags = DecryptUtil.getTags(metaData, null);
                FilePolicy policy = DecryptUtil.getFilePolicy(metaData, null);
                List<FilePolicy.Policy> adhocPolicies = policy.getPolicies();
                if (adhocPolicies != null && !adhocPolicies.isEmpty()) {
                    rights = Rights.fromInt(nxl.getPermissions());
                    validity = AdhocEvalAdapter.getFirstPolicyExpiry(policy);
                    file.setProtectionType(Constants.ProtectionType.ADHOC.ordinal());
                } else if (EvaluationAdapterFactory.isInitialized()) {
                    file.setProtectionType(Constants.ProtectionType.CENTRAL.ordinal());
                    com.nextlabs.rms.eval.User userEval = new com.nextlabs.rms.eval.User.Builder().id(String.valueOf(principal.getUserId())).ticket(principal.getTicket()).clientId(principal.getClientId()).platformId(principal.getPlatformId()).deviceId(principal.getDeviceId()).email(principal.getEmail()).displayName(principal.getName()).build();
                    List<EvalResponse> responses = CentralPoliciesEvaluationHandler.getCentralPolicyEvaluationResponse(nxl.getFilePathSearchSpace(), recipientMembership, nxl.getProject().getParentTenant().getName(), userEval, tags);
                    EvalResponse evalResponse = PolicyEvalUtil.getFirstAllowResponse(responses);
                    rights = evalResponse.getRights();
                }
            }
            file.setTags(tags);

            file.setDuid(nxl.getDuid());
            String fileName = FileUtils.getName(nxl.getFilePathDisplay());
            file.setFileName(fileName);
            file.setFileType(RepositoryFileUtil.getOriginalFileExtension(fileName));
            if (st.getCreationTime() != null) {
                file.setSharedDate(st.getCreationTime().getTime());
            }
            file.setSharedBy(st.getUser().getEmail());
            file.setTransactionId(st.getId());
            file.setTransactionCode(SharedFileManager.getTransactionCode(st.getId()));
            file.setRights(Rights.toStrings(rights));
            file.setValidity(validity);
            file.setComment(StringEscapeUtils.escapeHtml4(st.getComment()));
            file.setOwner(false);
            return file;
        } catch (IOException | NxlException | RepositoryException | InvalidDefaultRepositoryException
                | GeneralSecurityException e) {
            throw new RMSException(e.getMessage(), e);
        }
    }

    public static DefaultRepositoryTemplate getProjectRepository(DbSession session, RMSUserPrincipal userPrincipal,
        int projectId) throws UnauthorizedRepositoryException, InvalidDefaultRepositoryException {
        Criteria criteria = session.createCriteria(UserSession.class);
        criteria.add(Restrictions.eq("user.id", userPrincipal.getUserId()));
        criteria.add(Restrictions.eq("clientId", userPrincipal.getClientId()));
        criteria.add(Restrictions.eq("status", UserSession.Status.ACTIVE));
        UserSession us = (UserSession)criteria.uniqueResult();
        if (us == null) {
            throw new UnauthorizedRepositoryException(RMSMessageHandler.getClientString("unauthorizedProjectMemberErr"));
        }
        return new DefaultRepositoryProject(session, userPrincipal, projectId);
    }

    @Override
    public boolean revokeFile(String duid, int userId) throws FileAlreadyRevokedException {
        try (DbSession session = DbSession.newSession()) {
            session.beginTransaction();
            Criteria criteria = session.createCriteria(ProjectSpaceItem.class);
            criteria.add(Restrictions.eq("duid", duid));
            ProjectSpaceItem item = (ProjectSpaceItem)criteria.uniqueResult();
            if (item == null) {
                return false;
            }
            final Date now = new Date();
            item.setStatus(ProjectSpaceItem.Status.REVOKED);
            item.setLastModified(now);
            session.update(item);

            session.createQuery(UPDATE_SHARING_TRANSACTION).setString("duid", duid).executeUpdate();
            session.createQuery(UPDATE_SHARING_RECIPIENTS_STATUS_FLAG).setString("duid", duid).executeUpdate();
            session.commit();
            return true;
        }
    }

    private static Map<Integer, String> getAllProjectNames(DbSession session, int projectId) {
        Project project = session.get(Project.class, projectId);
        String tenantId = project.getParentTenant().getId();
        Criteria criteria = session.createCriteria(Project.class);
        criteria.add(Restrictions.eq("parentTenant.id", tenantId));
        List<Project> projectList = criteria.list();
        Map<Integer, String> projectIdNamesMap = new HashMap<>();
        for (Project projectThis : projectList) {
            projectIdNamesMap.put(projectThis.getId(), projectThis.getName());
        }
        return projectIdNamesMap;
    }

}
