package com.catedra.catalogo.service;

import com.catedra.catalogo.dto.CatalogSnapshotDTO;
import com.catedra.catalogo.dto.SyncStatusDTO;

public interface SyncService {

    CatalogSnapshotDTO syncSnapshot();

    void processIncrementalSync();

    SyncStatusDTO getStatus();
}
