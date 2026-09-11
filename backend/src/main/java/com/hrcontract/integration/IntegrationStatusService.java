package com.hrcontract.integration;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IntegrationStatusService {
    private final DirectoryProvider directory;
    private final FileStorageProvider files;
    private final ApprovalProvider approval;
    private final AiProvider ai;
    private final DocumentCenterProperties documentCenter;

    public IntegrationStatusService(DirectoryProvider directory, FileStorageProvider files, ApprovalProvider approval, AiProvider ai, DocumentCenterProperties documentCenter) {
        this.directory = directory;
        this.files = files;
        this.approval = approval;
        this.ai = ai;
        this.documentCenter = documentCenter;
    }

    public List<ExternalIntegrationStatus> statuses() {
        return List.of(
                status("HR_SSO_DIRECTORY", directory.name(), directory.testConnection()),
                fileStatus(files),
                status("APPROVAL", approval.name(), approval.testConnection()),
                status("AI", ai.name(), ai.testConnection()));
    }

    private ExternalIntegrationStatus fileStatus(FileStorageProvider files) {
        if (documentCenter.configured()) {
            return new ExternalIntegrationStatus("FILE_STORAGE", files.name(), files.mode(), files.testConnection(),
                    "Document-center registration adapter is ready. Enable document provider only after a binary-upload client that returns fileId and a service token are configured.");
        }
        return new ExternalIntegrationStatus("FILE_STORAGE", files.name(), files.mode(), files.testConnection(),
                "Local file fallback is active. Configure gateway URL, application code, library ID, and the document-center binary upload API before switching storage.");
    }

    private ExternalIntegrationStatus status(String capability, String provider, boolean available) {
        String mode = "mock-directory".equals(provider) ? "LOCAL_FALLBACK" : "MOCK";
        String note = "HR_SSO_DIRECTORY".equals(capability)
                ? "Local directory fallback is active. Configure a real HR/IAM adapter only after its authentication, user, organization and department contract is supplied."
                : "Replace the provider implementation after the external API contract and credentials are supplied.";
        return new ExternalIntegrationStatus(capability, provider, mode, available, note);
    }
}
