package dev.flanzo.bbsmodellights.client;

import dev.flanzo.bbsmodellights.form.LightForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

/** Invisible in-world, with a native BBS light icon in palette previews. */
public final class LightFormRenderer extends FormRenderer<LightForm> {
    public LightFormRenderer(LightForm form) {
        super(form);
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2) {
        context.batcher.icon(Icons.LIGHT, Colors.WHITE, (x1 + x2) / 2, (y1 + y2) / 2, 0.5F, 0.5F);
    }

    @Override
    protected void render3D(FormRenderingContext context) {
        /* A light form has no visible geometry, exactly like CML. */
    }
}
