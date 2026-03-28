package com.stockanalyzer.controller;

import com.stockanalyzer.common.ApiResponse;
import com.stockanalyzer.dto.IndustryDetail;
import com.stockanalyzer.dto.SectorDetail;
import com.stockanalyzer.dto.SectorOverview;
import com.stockanalyzer.service.SectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SectorController {

    private final SectorService sectorService;

    @Autowired
    public SectorController(SectorService sectorService) {
        this.sectorService = sectorService;
    }

    @GetMapping("/sectors")
    public ApiResponse<List<SectorOverview>> getAllSectors() {
        return ApiResponse.ok(sectorService.getAllSectors());
    }

    @GetMapping("/sectors/{id}")
    public ApiResponse<SectorDetail> getSectorDetail(@PathVariable Long id) {
        SectorDetail detail = sectorService.getSectorDetail(id);
        if (detail == null) {
            return ApiResponse.error("Sector not found: " + id);
        }
        return ApiResponse.ok(detail);
    }

    @GetMapping("/sectors/{id}/rankings")
    public ApiResponse<List<SectorOverview>> getSectorRankings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "gainers") String sort) {
        return ApiResponse.ok(sectorService.getSectorRankings(id, sort));
    }

    @GetMapping("/sectors/compare")
    public ApiResponse<List<SectorOverview>> compareSectors() {
        return ApiResponse.ok(sectorService.getAllSectors());
    }

    @GetMapping("/industries/{id}")
    public ApiResponse<IndustryDetail> getIndustryDetail(@PathVariable Long id) {
        IndustryDetail detail = sectorService.getIndustryDetail(id);
        if (detail == null) {
            return ApiResponse.error("Industry not found: " + id);
        }
        return ApiResponse.ok(detail);
    }
}
