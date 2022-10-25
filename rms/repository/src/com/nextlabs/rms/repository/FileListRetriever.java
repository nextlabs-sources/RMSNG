package com.nextlabs.rms.repository;

import com.nextlabs.rms.json.Repository;
import com.nextlabs.rms.repository.exception.InvalidTokenException;
import com.nextlabs.rms.repository.exception.RepositoryAccessException;
import com.nextlabs.rms.repository.exception.UnauthorizedRepositoryException;

import java.util.List;
import java.util.concurrent.Callable;
public class FileListRetriever implements Callable<List<RepositoryContent>> {

    private IRepository repository;

    public FileListRetriever(IRepository repository) {
        this.repository = repository;
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Override
    public List<RepositoryContent> call() throws Exception {
        List<RepositoryContent> fileList = null;
        try {
            fileList = repository.getFileList("/");
        } catch (InvalidTokenException e) {
            Repository repo = new Repository();
            repo.setRepoId(repository.getRepoId());
            repo.setRepoName(repository.getRepoName());
            repo.setRepoType(repository.getRepoType().name());
            throw new InvalidTokenException(e.getMessage(), e, repo);
        } catch (UnauthorizedRepositoryException ue) {
            throw new UnauthorizedRepositoryException(ue.getMessage(), ue, repository.getRepoName());
        } catch (Exception ex) {
            throw new RepositoryAccessException(ex.getMessage(), ex, repository.getRepoName());
        }
        return fileList;
    }

}
