package dasarathi.devops.toolkit.gui.repositories;

public record FilterProperties(
    String allUsers,
    String selectedUser,
    String allStatuses,
    String selectedStatus,
    String allBranches,
    String selectedBranch,
    String sortOldest,
    String selectedSort,
    String unknownUser,
    String unknownBranch,
    String query) {}
