package com.nextlabs.rms.application.sharepoint;

public class SharePointApplicationRepositoryTest {

    //    private static final String TENANT_ID = "82fa1a12-0ec4-4393-a078-28136ca3f6df";
    //    private static final String CLIENT_ID = "35e0eff9-4c4d-4bf0-9896-5a2f8824470b";
    //    private static final String CLIENT_SECRET = "1e~20heuj~vf.jhQsea-vOZ0Yefs_n2zk0";

    //        public static void main(String[] args) {
    //            try {
    //                SharePointApplicationRepository repository = getConfigurableRepo();
    //                List<ApplicationRepositoryContent> data = repository.getFileList("/folder-one", new FilterOptions());
    //                System.out.println(data.size());
    //            } catch (ApplicationRepositoryException | RepositoryException e) {
    //                e.printStackTrace();
    //            }
    //        }

    //        public static void main(String[] args) {
    //            try {
    //                String output = "C:\\Users\\hhu\\Downloads";
    //                SharePointApplicationRepository repository = getConfigurableRepo();
    //                File f = repository.getFile("", "/folder-one/thor.jpg", output);
    //
    //                System.out.println(f.length());
    //            } catch (ApplicationRepositoryException | IOException | RepositoryException e) {
    //                e.printStackTrace();
    //            }
    //        }

    //        public static void main(String[] args) {
    //            File src = new File("C:\\Users\\hhu\\Pictures\\Saved Pictures\\odsp\\test.png");
    //            try {
    //                SharePointApplicationRepository repository = getConfigurableRepo();
    //                FileUploadMetadata metadata = repository.uploadFile("", "/folder-one", src.getPath(), true, "JUnitTest_DeleteItem(1).jpg", null);
    //                System.out.println(metadata);
    //            } catch (ApplicationRepositoryException | RepositoryException e) {
    //                e.printStackTrace();
    //            }
    //        }

    //        public static void main(String[] args) {
    //            try {
    //                SharePointApplicationRepository repository = getConfigurableRepo();
    //                ApplicationRepositoryContent metadata = repository.getFileMetadata("/folder-one/test.png");
    //                System.out.println(metadata);
    //            } catch (ApplicationRepositoryException | RepositoryException e) {
    //                e.printStackTrace();
    //            }
    //        }

    //        public static void main(String[] args) {
    //            try {
    //                SharePointApplicationRepository repository = getConfigurableRepo();
    //                byte[] data = repository.downloadPartialFile("", "/folder-one/test.png", 0, 0x4000);
    //                System.out.println(data.length);
    //            } catch (ApplicationRepositoryException | RepositoryException e) {
    //                e.printStackTrace();
    //            }
    //        }

    //        public static void main(String[] args) {
    //            try {
    //                SharePointApplicationRepository repository = getConfigurableRepo();
    //                FileDeleteMetaData metaData = repository.deleteFile("", "/folder-one/test.png");
    //                System.out.println(metaData);
    //            } catch (ApplicationRepositoryException | RepositoryException e) {
    //                e.printStackTrace();
    //            }
    //        }

    //        public static void main(String[] args) {
    //            try {
    //                SharePointApplicationRepository repository = getConfigurableRepo();
    //                List<ApplicationRepositoryContent> data = repository.search("thor");
    //                System.out.println(data.size());
    //            } catch (ApplicationRepositoryException | RepositoryException e) {
    //                e.printStackTrace();
    //            }
    //        }

    //    public static void main(String[] args) {
    //        try {
    //            SharePointApplicationRepository repository = getConfigurableRepo();
    //            boolean exists = repository.checkIfFileExists("/folder-one/thor.jpg");
    //            assert exists;
    //        } catch (ApplicationRepositoryException | RepositoryException e) {
    //            e.printStackTrace();
    //        }
    //    }

    //    private static SharePointApplicationRepository getConfigurableRepo()
    //            throws ApplicationRepositoryException, RepositoryException {
    //        SharePointApplicationRepository repository = new SharePointApplicationRepository(TENANT_ID, CLIENT_ID, CLIENT_SECRET);
    //        String siteUrl = "https://skydrmomega.sharepoint.com/sites/skydrmomega/subsite01";
    //        String driveName = "doclib02";
    //        repository.configDrive(siteUrl, driveName);
    //        return repository;
    //    }

}
