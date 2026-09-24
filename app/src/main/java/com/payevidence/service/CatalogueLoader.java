package com.payevidence.service;

import com.payevidence.domain.ArtifactType;
import com.payevidence.domain.Control;
import com.payevidence.repo.ControlRepository;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

@Service
public class CatalogueLoader {
  private static final Logger log = LoggerFactory.getLogger(CatalogueLoader.class);
  private final ControlRepository controlRepository;

  public CatalogueLoader(ControlRepository controlRepository) {
    this.controlRepository = controlRepository;
  }

  @Transactional
  @SuppressWarnings("unchecked")
  public List<Control> loadFromYaml(Path path) {
    try (InputStream in = Files.newInputStream(path)) {
      Yaml yaml = new Yaml();
      Map<String, Object> root = yaml.load(in);
      Map<String, Object> catalogue = (Map<String, Object>) root.get("catalogue");
      String catalogueId = catalogue != null ? String.valueOf(catalogue.get("id")) : "pci-mvp-v0";
      List<Map<String, Object>> controls = (List<Map<String, Object>>) root.get("controls");
      List<Control> saved = new ArrayList<>();
      for (Map<String, Object> c : controls) {
        Control entity = controlRepository.findById(String.valueOf(c.get("id"))).orElse(new Control());
        entity.setId(String.valueOf(c.get("id")));
        entity.setCatalogueId(catalogueId);
        entity.setPciRef(str(c.get("pci_ref")));
        entity.setTitle(str(c.get("title")));
        entity.setDescription(str(c.get("description")));
        entity.setFreshnessDays(c.get("freshness_days") instanceof Number n ? n.intValue() : 30);
        entity.setPassRule(str(c.get("pass_rule")));
        List<ArtifactType> types = new ArrayList<>();
        Object et = c.get("evidence_types");
        if (et instanceof List<?> list) {
          for (Object t : list) {
            types.add(ArtifactType.valueOf(String.valueOf(t)));
          }
        }
        entity.setEvidenceTypes(types);
        saved.add(controlRepository.save(entity));
      }
      log.info("Loaded {} controls from catalogue {}", saved.size(), catalogueId);
      return saved;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load catalogue from " + path, e);
    }
  }

  private static String str(Object o) {
    return o == null ? null : String.valueOf(o).trim();
  }
}
