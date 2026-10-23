package com.catedra.catalogo.service;

import com.catedra.catalogo.dto.CatalogSnapshotDTO;

public interface SyncService {
    CatalogSnapshotDTO syncSnapshot();
}