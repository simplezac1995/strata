// SPDX-License-Identifier: Apache-2.0
// One provider row in the left rail.

import { Box, Stack, Typography } from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import { Models } from "./main";

export function ProviderRow({
  active,
  name,
  sub,
  onClick,
  onAdd,
}: {
  active: boolean;
  name: string;
  sub: string;
  onClick: () => void;
  onAdd?: () => void;
}) {
  return (
    <Stack
      direction="row"
      sx={{
        alignItems: "center",
        gap: 1.25,
        px: 1.25,
        py: 1,
        mb: 0.5,
        borderRadius: 1,
        cursor: "pointer",
        bgcolor: active ? "action.selected" : "transparent",
        "&:hover": { bgcolor: "action.hover" },
      }}
      onClick={onClick}
    >
      <Box
        sx={{
          width: 32,
          height: 32,
          borderRadius: 1.5,
          flexShrink: 0,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          bgcolor: "primary.light",
          color: "primary.contrastText",
          fontSize: 14,
          fontWeight: 700,
        }}
      >
        {name.slice(0, 1)}
      </Box>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="body2" sx={{ fontWeight: 600, lineHeight: 1.3 }} noWrap>
          {name}
        </Typography>
        <Typography variant="caption" sx={{ color: "text.secondary" }} noWrap>
          {sub}
        </Typography>
      </Box>
      {onAdd ? (
        <Box
          component="span"
          onClick={(e) => {
            e.stopPropagation();
            onAdd();
          }}
          sx={{
            display: "inline-flex",
            alignItems: "center",
            justifyContent: "center",
            width: 28,
            height: 28,
            borderRadius: 1,
            color: "text.secondary",
            "&:hover": { bgcolor: "action.hover", color: "primary.main" },
          }}
        >
          <AddIcon sx={{ fontSize: 18 }} />
        </Box>
      ) : null}
    </Stack>
  );
}

export default Models;
