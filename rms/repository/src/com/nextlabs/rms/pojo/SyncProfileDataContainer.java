/**
 *
 */
package com.nextlabs.rms.pojo;

import com.nextlabs.common.shared.JsonRepository;
import com.nextlabs.rms.hibernate.model.Repository;

import java.util.List;

/**
 * @author nnallagatla
 * This class acts as container for the data that needs to be sent as part of sync profile request
 *
 */
public class SyncProfileDataContainer {

    private boolean isFullCopy;
    private List<Repository> repositoryList;
    private List<JsonRepository> jsonRepositoryList;
    private List<String> deletedRepositoryList;

    public SyncProfileDataContainer() {
    }

    public List<String> getDeletedRepositoryList() {
        return deletedRepositoryList;
    }

    public List<Repository> getRepositoryList() {
        return repositoryList;
    }

    public boolean isFullCopy() {
        return isFullCopy;
    }

    public void setDeletedRepositoryList(List<String> deletedRepositoryList) {
        this.deletedRepositoryList = deletedRepositoryList;
    }

    public void setFullCopy(boolean isFullCopy) {
        this.isFullCopy = isFullCopy;
    }

    public void setRepositoryList(List<Repository> repositoryList) {
        this.repositoryList = repositoryList;
    }

    public void setRepositoryJsonList(List<JsonRepository> repoList) {
        this.jsonRepositoryList = repoList;
    }

    public List<JsonRepository> getRepositoryJsonList() {
        return jsonRepositoryList;
    }

}
