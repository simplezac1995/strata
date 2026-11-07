// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.seed;

import dev.strata.system.api.seed.OpsMenuSeedContext;
import dev.strata.system.api.seed.OpsMenuSeedContributor;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Owns the whole AI menu subtree (ADR-0020 decision 3: system seed keeps only system/demo).
 *
 * <p>Self-contained: ensures the {@code /ai} directory itself instead of relying on system menus.json
 * having planted it, then adds every CRUD page (mirroring the former menus.json entries, semantic-key
 * names preserved) plus the AI Tools action menu. Idempotent per permission marker.
 */
@Component
public class AiOpsMenuSeedContributor implements OpsMenuSeedContributor {

    private record Page(String keyName, String path, String component, String permissionPrefix, int sortOrder) {}

    private static final List<Page> PAGES = List.of(
            new Page("nav.providers", "providers", "ai/Providers", "ai:provider", 1),
            new Page("nav.models", "models", "ai/Models", "ai:model", 2),
            new Page("nav.vector.stores", "vector-stores", "ai/VectorStores", "ai:vectorstore", 3),
            new Page("nav.prompts", "prompts", "ai/Prompts", "ai:prompt", 4),
            new Page("nav.terminology", "terminology", "ai/Terminology", "ai:term", 5),
            new Page("nav.knowledge.bases", "knowledge-bases", "ai/KnowledgeBases", "ai:knowledge", 6),
            new Page("nav.documents", "documents", "ai/Documents", "ai:document", 7),
            new Page("nav.skills", "skills", "ai/Skills", "ai:skill", 8),
            new Page("nav.interface.skills", "interface-skills", "ai/InterfaceSkills", "ai:skill", 9),
            new Page("nav.mcp.servers", "mcp-servers", "ai/McpServers", "ai:mcp", 10),
            new Page("nav.agents", "agents", "ai/Agents", "ai:agent", 11),
            new Page("nav.datasets", "datasets", "ai/Datasets", "ai:dataset", 12),
            new Page("nav.chat", "chat", "ai/Chat", "ai:chat", 13),
            new Page("nav.ai.linkage", "linkage", "ai/LinkageRules", "ai:linkage", 15));

    @Override
    public void seed(OpsMenuSeedContext context) {
        long aiDirId = context.ensureDirectory("nav.ai", "/ai", 4, null);
        List<Long> created = new ArrayList<>();
        for (Page page : PAGES) {
            if (!context.existsChild(aiDirId, page.path())) {
                created.addAll(context.addMenuWithButtons(
                        page.keyName(),
                        page.path(),
                        page.component(),
                        page.permissionPrefix(),
                        aiDirId,
                        page.sortOrder()));
            }
        }
        if (!context.existsChild(aiDirId, "tools-run")) {
            long toolParent = context.addMenu("nav.ai.tools", "tools-run", "ai/Tools", "ai:tool:list", aiDirId, 14);
            created.add(toolParent);
            created.add(context.addButton("nav.ai.tools.run", "ai:tool:create", toolParent, 1));
        }
        context.linkAdminRole(created);
    }
}
