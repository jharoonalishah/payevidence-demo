package com.payevidence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.payevidence.config.PayEvidenceProperties;
import com.payevidence.domain.*;
import com.payevidence.repo.ArtifactRepository;
import com.payevidence.repo.ControlStatusRepository;
import com.payevidence.repo.EvidencePackRepository;
import com.payevidence.web.error.NotFoundException;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidencePackService {
  private static final DateTimeFormatter STAMP =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
  private static final DateTimeFormatter DISPLAY =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

  private final EvidencePackRepository packRepository;
  private final ControlStatusRepository statusRepository;
  private final ArtifactRepository artifactRepository;
  private final PayEvidenceProperties props;
  private final ObjectMapper objectMapper;

  public EvidencePackService(
      EvidencePackRepository packRepository,
      ControlStatusRepository statusRepository,
      ArtifactRepository artifactRepository,
      PayEvidenceProperties props,
      ObjectMapper objectMapper) {
    this.packRepository = packRepository;
    this.statusRepository = statusRepository;
    this.artifactRepository = artifactRepository;
    this.props = props;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public EvidencePack createPack(Organization org) {
    List<ControlStatus> statuses = statusRepository.findByOrganization(org);
    List<Artifact> artifacts = artifactRepository.findByOrganization(org);
    Instant now = Instant.now();
    String stamp = STAMP.format(now);
    String displayTs = DISPLAY.format(now);

    ObjectNode snapshot = objectMapper.createObjectNode();
    snapshot.put("organization", org.getName());
    snapshot.put("organizationId", org.getId().toString());
    snapshot.put("createdAt", now.toString());
    snapshot.put("generatedAtDisplay", displayTs);
    ArrayNode arr = snapshot.putArray("controls");
    long pass = 0, gap = 0, stale = 0;
    StringBuilder rows = new StringBuilder();
    for (ControlStatus cs : statuses) {
      ObjectNode row = arr.addObject();
      row.put("controlId", cs.getControl().getId());
      row.put("title", cs.getControl().getTitle());
      row.put("status", cs.getStatus().name());
      row.put("reason", cs.getReason());
      row.put("computedAt", cs.getComputedAt().toString());
      switch (cs.getStatus()) {
        case pass -> pass++;
        case gap -> gap++;
        case stale -> stale++;
      }
      rows.append("<tr><td>").append(esc(cs.getControl().getId()))
          .append("</td><td>").append(esc(cs.getControl().getTitle()))
          .append("</td><td class=\"").append(cs.getStatus().name()).append("\">")
          .append(cs.getStatus().name())
          .append("</td><td>").append(esc(cs.getReason()))
          .append("</td></tr>\n");
    }
    snapshot.put("pass", pass);
    snapshot.put("gap", gap);
    snapshot.put("stale", stale);
    snapshot.put("controlCount", statuses.size());

    ArrayNode arts = snapshot.putArray("artifacts");
    StringBuilder artRows = new StringBuilder();
    for (Artifact a : artifacts) {
      ObjectNode n = arts.addObject();
      n.put("id", a.getId().toString());
      n.put("type", a.getType().name());
      n.put("capturedAt", a.getCapturedAt().toString());
      n.put("storagePath", a.getStoragePath());
      artRows.append("<tr><td>").append(esc(a.getType().name()))
          .append("</td><td>").append(esc(a.getCapturedAt().toString()))
          .append("</td><td>").append(esc(a.getId().toString()))
          .append("</td><td>").append(esc(a.getStoragePath()))
          .append("</td></tr>\n");
    }
    snapshot.put("artifactCount", artifacts.size());

    String html = """
        <!DOCTYPE html>
        <html lang="en"><head><meta charset="utf-8">
        <title>PayEvidence Evidence Pack — %s</title>
        <style>
          body{font-family:system-ui,-apple-system,sans-serif;margin:2rem;color:#1a1a1a;line-height:1.45}
          h1{margin-bottom:.25rem}
          h2{margin-top:2rem;border-bottom:2px solid #e5e5e5;padding-bottom:.35rem}
          .banner{background:#0b3d5c;color:#fff;padding:1rem 1.25rem;border-radius:8px;margin-bottom:1.25rem}
          .banner h1{color:#fff;font-size:1.35rem;margin:0}
          .banner .sub{opacity:.9;font-size:.95rem;margin-top:.35rem}
          .summary{display:flex;gap:1rem;flex-wrap:wrap;margin:1rem 0}
          .chip{padding:.5rem .9rem;border-radius:6px;font-weight:600;font-size:.95rem}
          .chip.pass{background:#e6f7f0;color:#0a7a4f}
          .chip.gap{background:#fdecea;color:#b00020}
          .chip.stale{background:#fff4e5;color:#9a6700}
          .chip.total{background:#eef2f6;color:#334}
          table{border-collapse:collapse;width:100%%;margin:1rem 0}
          th,td{border:1px solid #ccc;padding:.45rem .65rem;text-align:left;font-size:13px;vertical-align:top}
          th{background:#f4f6f8}
          .pass{color:#0a7a4f;font-weight:600}.gap{color:#b00020;font-weight:600}.stale{color:#9a6700;font-weight:600}
          .meta{color:#555;font-size:.9rem}
          .disclaimer{margin-top:2rem;padding:.75rem 1rem;background:#f8f8f8;border-left:4px solid #0b3d5c;font-size:.85rem;color:#444}
        </style></head><body>
        <div class="banner">
          <h1>PayEvidence Evidence Pack</h1>
          <div class="sub">Organization: <strong>%s</strong> · Generated: %s</div>
        </div>
        <p class="meta">Pack id stamped at generation · Synthetic demo evidence · Not a QSA assessment · No CHD stored</p>
        <div class="summary">
          <span class="chip total">Controls: %d</span>
          <span class="chip pass">pass: %d</span>
          <span class="chip gap">gap: %d</span>
          <span class="chip stale">stale: %d</span>
          <span class="chip total">Artifacts: %d</span>
        </div>
        <h2>Control status</h2>
        <table><thead><tr><th>Control ID</th><th>Title</th><th>Status</th><th>Reason</th></tr></thead>
        <tbody>
        %s
        </tbody></table>
        <h2>Artifacts</h2>
        <table><thead><tr><th>Type</th><th>Captured at</th><th>Artifact ID</th><th>Storage path</th></tr></thead>
        <tbody>
        %s
        </tbody></table>
        <div class="disclaimer">
          PayEvidence continuous evidence snapshot for <strong>%s</strong>.
          Generated %s. For demo / design-partner use only.
        </div>
        </body></html>
        """.formatted(
        esc(org.getName()),
        esc(org.getName()),
        esc(displayTs),
        statuses.size(),
        pass,
        gap,
        stale,
        artifacts.size(),
        rows,
        artRows,
        esc(org.getName()),
        esc(displayTs));

    EvidencePack pack = new EvidencePack();
    pack.setOrganization(org);
    pack.setCreatedAt(now);
    try {
      pack.setStatusesJson(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot));
    } catch (Exception e) {
      pack.setStatusesJson("{}");
    }
    pack.setHtmlContent(html);

    Path dir = Path.of(props.getPacksDir()).toAbsolutePath().normalize();
    try {
      Files.createDirectories(dir);
      String base = "pack-" + org.getName() + "-" + stamp;
      Path htmlOut = dir.resolve(base + ".html");
      Files.writeString(htmlOut, html);
      pack.setStoragePath(htmlOut.toString());

      byte[] pdfBytes = renderPdf(org.getName(), displayTs, pass, gap, stale, statuses, artifacts);
      Path pdfOut = dir.resolve(base + ".pdf");
      Files.write(pdfOut, pdfBytes);
      pack.setPdfStoragePath(pdfOut.toString());
    } catch (Exception e) {
      // HTML still available in DB even if disk write fails
      if (pack.getStoragePath() == null) {
        pack.setStoragePath(null);
      }
    }
    return packRepository.save(pack);
  }

  @Transactional(readOnly = true)
  public EvidencePack getPack(UUID packId) {
    return packRepository.findById(packId)
        .orElseThrow(() -> new NotFoundException("Evidence pack not found: " + packId));
  }

  @Transactional(readOnly = true)
  public byte[] getPdfBytes(EvidencePack pack) {
    if (pack.getPdfStoragePath() != null) {
      try {
        Path p = Path.of(pack.getPdfStoragePath());
        if (Files.exists(p)) {
          return Files.readAllBytes(p);
        }
      } catch (Exception ignored) {
        // fall through to regenerate
      }
    }
    try {
      var snapshot = objectMapper.readTree(pack.getStatusesJson());
      String orgName = snapshot.path("organization").asText(pack.getOrganization().getName());
      String displayTs = snapshot.path("generatedAtDisplay").asText(DISPLAY.format(pack.getCreatedAt()));
      long pass = snapshot.path("pass").asLong(0);
      long gap = snapshot.path("gap").asLong(0);
      long stale = snapshot.path("stale").asLong(0);
      List<ControlStatus> statuses = statusRepository.findByOrganization(pack.getOrganization());
      List<Artifact> artifacts = artifactRepository.findByOrganization(pack.getOrganization());
      return renderPdf(orgName, displayTs, pass, gap, stale, statuses, artifacts);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to render PDF for pack " + pack.getId(), e);
    }
  }

  private byte[] renderPdf(
      String orgName,
      String displayTs,
      long pass,
      long gap,
      long stale,
      List<ControlStatus> statuses,
      List<Artifact> artifacts) throws DocumentException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Document doc = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
    PdfWriter.getInstance(doc, baos);
    doc.open();

    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE);
    Font h2 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    Font body = FontFactory.getFont(FontFactory.HELVETICA, 9);
    Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

    PdfPTable banner = new PdfPTable(1);
    banner.setWidthPercentage(100);
    PdfPCell bannerCell = new PdfPCell();
    bannerCell.setBackgroundColor(new Color(11, 61, 92));
    bannerCell.setPadding(10);
    bannerCell.setBorder(0);
    bannerCell.addElement(new Paragraph("PayEvidence Evidence Pack", titleFont));
    bannerCell.addElement(new Paragraph(
        "Organization: " + orgName + "  ·  Generated: " + displayTs,
        FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE)));
    banner.addCell(bannerCell);
    doc.add(banner);
    doc.add(new Paragraph(" ", body));

    Paragraph summary = new Paragraph(
        String.format("Controls: %d   |   pass: %d   |   gap: %d   |   stale: %d   |   Artifacts: %d",
            statuses.size(), pass, gap, stale, artifacts.size()),
        h2);
    summary.setSpacingAfter(10);
    doc.add(summary);
    doc.add(new Paragraph("Synthetic demo evidence. Not a QSA assessment. No CHD.", small));
    doc.add(new Paragraph(" ", body));

    doc.add(new Paragraph("Control status", h2));
    PdfPTable controlTable = new PdfPTable(new float[]{2.4f, 2.6f, 0.8f, 4.2f});
    controlTable.setWidthPercentage(100);
    controlTable.setSpacingBefore(6);
    addHeader(controlTable, headerFont, "Control ID", "Title", "Status", "Reason");
    for (ControlStatus cs : statuses) {
      controlTable.addCell(cell(cs.getControl().getId(), body));
      controlTable.addCell(cell(cs.getControl().getTitle(), body));
      controlTable.addCell(statusCell(cs.getStatus().name(), body));
      controlTable.addCell(cell(cs.getReason() != null ? cs.getReason() : "", body));
    }
    doc.add(controlTable);

    doc.add(new Paragraph(" ", body));
    doc.add(new Paragraph("Artifacts", h2));
    PdfPTable artTable = new PdfPTable(new float[]{2f, 2.2f, 2.5f, 3.3f});
    artTable.setWidthPercentage(100);
    artTable.setSpacingBefore(6);
    addHeader(artTable, headerFont, "Type", "Captured at", "Artifact ID", "Storage path");
    for (Artifact a : artifacts) {
      artTable.addCell(cell(a.getType().name(), body));
      artTable.addCell(cell(a.getCapturedAt().toString(), body));
      artTable.addCell(cell(a.getId().toString(), body));
      artTable.addCell(cell(a.getStoragePath() != null ? a.getStoragePath() : "", body));
    }
    doc.add(artTable);

    Paragraph footer = new Paragraph(
        "PayEvidence continuous evidence snapshot for " + orgName + ". Generated " + displayTs + ".",
        small);
    footer.setSpacingBefore(16);
    doc.add(footer);
    doc.close();
    return baos.toByteArray();
  }

  private static void addHeader(PdfPTable table, Font font, String... labels) {
    for (String label : labels) {
      PdfPCell c = new PdfPCell(new Phrase(label, font));
      c.setBackgroundColor(new Color(11, 61, 92));
      c.setPadding(4);
      c.setHorizontalAlignment(Element.ALIGN_LEFT);
      table.addCell(c);
    }
  }

  private static PdfPCell cell(String text, Font font) {
    PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "", font));
    c.setPadding(3);
    return c;
  }

  private static PdfPCell statusCell(String status, Font font) {
    PdfPCell c = cell(status, font);
    if ("pass".equals(status)) c.setBackgroundColor(new Color(230, 247, 240));
    else if ("gap".equals(status)) c.setBackgroundColor(new Color(253, 236, 234));
    else if ("stale".equals(status)) c.setBackgroundColor(new Color(255, 244, 229));
    return c;
  }

  private static String esc(String s) {
    if (s == null) return "";
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
  }
}
