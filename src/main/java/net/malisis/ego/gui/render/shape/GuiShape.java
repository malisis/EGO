/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Ordinastie
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.malisis.ego.gui.render.shape;

import static com.google.common.base.Preconditions.*;

import com.google.common.collect.Maps;
import net.malisis.ego.gui.component.UIComponent;
import net.malisis.ego.gui.component.content.IContent;
import net.malisis.ego.gui.element.IChild;
import net.malisis.ego.gui.element.IOffset;
import net.malisis.ego.gui.element.position.IPositionBuilder;
import net.malisis.ego.gui.element.position.Position;
import net.malisis.ego.gui.element.position.Position.IPosition;
import net.malisis.ego.gui.element.position.Position.ScreenPosition;
import net.malisis.ego.gui.element.size.ISizeBuilder;
import net.malisis.ego.gui.element.size.Size;
import net.malisis.ego.gui.element.size.Size.ISize;
import net.malisis.ego.gui.element.size.Sizes;
import net.malisis.ego.gui.render.GuiIcon;
import net.malisis.ego.gui.render.GuiRenderer;
import net.malisis.ego.gui.theme.Theme;

import java.util.EnumMap;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;

import javax.annotation.Nonnull;

/**
 * @author Ordinastie
 */
public class GuiShape implements IContent, IChild
{
	public static final int CORNER_SIZE = 5;

	private final Object parent;
	private final IPosition position;
	private final ScreenPosition screenPosition;
	private final IntSupplier zIndex;
	private final ISize size;
	private final ToIntBiFunction<FacePosition, VertexPosition> color;
	private final ToIntBiFunction<FacePosition, VertexPosition> alpha;
	private final Supplier<GuiIcon> icon;
	private final int border;
	private final boolean fixed;

	private GuiShape(Object parent, Function<GuiShape, IPosition> position, IntSupplier zIndex, Function<GuiShape, ISize> size, ToIntBiFunction<FacePosition, VertexPosition> color, ToIntBiFunction<FacePosition, VertexPosition> alpha, Supplier<GuiIcon> icon, int border, boolean fixed)
	{
		this.parent = parent;
		this.position = position.apply(this);
		screenPosition = new ScreenPosition(this);
		this.zIndex = zIndex;
		this.size = size.apply(this);
		this.color = color;
		this.alpha = alpha;
		this.icon = icon;
		this.border = border;
		this.fixed = fixed;
	}

	@Nonnull
	@Override
	public IPosition position()
	{
		return position;
	}

	@Override
	public boolean fixed()
	{
		return fixed;
	}

	public int getZIndex()
	{
		if (zIndex != null)
			return zIndex.getAsInt();
		if (parent instanceof UIComponent)
			return ((UIComponent) parent).zIndex();
		return 0;
	}

	@Nonnull
	@Override
	public ISize size()
	{
		return size;
	}

	@Override
	public Object getParent()
	{
		return parent;
	}

	public int getColor(FacePosition facePosition, VertexPosition vertexPosition)
	{
		if (color == null)
			return 0xFFFFFF;
		return color.applyAsInt(facePosition, vertexPosition);
	}

	public int getAlpha(FacePosition facePosition, VertexPosition vertexPosition)
	{
		if (alpha == null)
			return 255;
		return alpha.applyAsInt(facePosition, vertexPosition);
	}

	public GuiIcon getIcon()
	{
		if (icon == null || icon.get() == null)
			return GuiIcon.NONE;
		return icon.get();
	}

	@Override
	public void render(GuiRenderer renderer)
	{
		render(renderer, screenPosition, size());
	}

	public void renderFor(GuiRenderer renderer, UIComponent t)
	{
		render(renderer, t.screenPosition(), t.size());
	}

	public void render(GuiRenderer renderer, IPosition position, ISize size)
	{
		getIcon().bind(renderer);
		for (FacePosition fp : FacePosition.VALUES)
			for (VertexPosition vp : VertexPosition.VALUES)
				addVertexData(fp, vp, position, size);
	}

	/**
	 * Gets the vertex data for this vertex.
	 *
	 * @param fp the fp
	 * @param vp the position
	 */
	public void addVertexData(FacePosition fp, VertexPosition vp, IPosition position, ISize size)
	{
		GuiIcon icon = getIcon();

		float x, y, u, v;
		if (border == 0)
		{
			if (fp != FacePosition.CENTER)
				return;

			x = vp.x(size.width());
			y = vp.y(size.height());
			u = icon.interpolatedU(vp.x());
			v = icon.interpolatedV(vp.y());
			//debug show full texture
			//			u = vp.x();
			//			v = vp.y();

		}
		else
		{
			int width = size.width() - 2 * border;
			int height = size.height() - 2 * border;

			x = fp.x(width, border) + vp.x(fp.width(width, border));
			y = fp.y(height, border) + vp.y(fp.height(height, border));

			u = border > 0 ? interpolatedU(fp, vp, icon) : icon.interpolatedU(x);
			v = border > 0 ? interpolatedV(fp, vp, icon) : icon.interpolatedV(y);
		}

		int color = getColor(fp, vp);
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = (color) & 0xFF;
		color = (b << 16) + (g << 8) + r + (getAlpha(fp, vp) << 24);

		GuiRenderer.BUFFER.addVertexData(new int[] { Float.floatToRawIntBits(position.x() + x),
													 Float.floatToRawIntBits(position.y() + y),
													 Float.floatToRawIntBits(getZIndex()),
													 Float.floatToRawIntBits(u),
													 Float.floatToRawIntBits(v),
													 color });
	}

	private float interpolatedU(FacePosition fp, VertexPosition vp, GuiIcon icon)
	{
		switch (fp.x() + vp.x())
		{
			case 1:
				return icon.pixelToU(border);
			case 2:
				return icon.pixelToU(-border);
			case 3:
				return icon.interpolatedU(1);
		}
		return icon.interpolatedU(0);
	}

	private float interpolatedV(FacePosition fp, VertexPosition vp, GuiIcon icon)
	{
		switch (fp.y() + vp.y())
		{
			case 1:
				return icon.pixelToV(border);
			case 2:
				return icon.pixelToV(-border);
			case 3:
				return icon.interpolatedV(1);
		}
		return icon.interpolatedV(0);
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static Builder builder(UIComponent component)
	{
		return new Builder().forComponent(component);
	}

	public static class Builder implements IPositionBuilder<Builder, GuiShape>, ISizeBuilder<Builder, GuiShape>
	{
		private Object parent;
		private boolean fixed = true;
		private Function<GuiShape, IntSupplier> x = o -> () -> 0;
		private Function<GuiShape, IntSupplier> y = o -> () -> 0;
		private Function<GuiShape, IPosition> position = o -> Position.of(x.apply(o), y.apply(o));
		private Function<GuiShape, IntSupplier> width = o -> () -> o.getIcon()
																	.width();
		private Function<GuiShape, IntSupplier> height = o -> () -> o.getIcon()
																	 .height();
		private Function<GuiShape, ISize> size = o -> Size.of(width.apply(o), height.apply(o));

		private IntSupplier zIndex;
		private ToIntBiFunction<FacePosition, VertexPosition> color = (fp, vp) -> 0xFFFFFF;
		private ToIntBiFunction<FacePosition, VertexPosition> alpha = (fp, vp) -> 255;
		private final EnumMap<VertexPosition, Integer> colors = Maps.newEnumMap(VertexPosition.class);
		private final EnumMap<VertexPosition, Integer> alphas = Maps.newEnumMap(VertexPosition.class);
		private Supplier<GuiIcon> icon = () -> GuiIcon.NONE;
		private int borderColor;
		private int borderAlpha;
		private int borderSize;

		public Builder forComponent(UIComponent component)
		{
			parent = component;
			width(o -> Sizes.widthRelativeTo(component, 1.0F, 0));
			height(o -> Sizes.heightRelativeTo(component, 1.0F, 0));
			color = (fp, vp) -> component.getColor();
			alpha = (fp, vp) -> component.getAlpha();
			zIndex = component::zIndex;
			return this;
		}

		public Builder parent(Object parent)
		{
			this.parent = parent;
			return this;
		}

		@Override
		public Builder position(Function<GuiShape, IPosition> func)
		{
			position = checkNotNull(func);
			return this;
		}

		@Override
		public Builder x(Function<GuiShape, IntSupplier> x)
		{
			this.x = checkNotNull(x);
			return this;
		}

		@Override
		public Builder y(Function<GuiShape, IntSupplier> y)
		{
			this.y = checkNotNull(y);
			return this;
		}

		@Override
		public Builder size(Function<GuiShape, ISize> func)
		{
			size = checkNotNull(func);
			return this;
		}

		@Override
		public Builder width(Function<GuiShape, IntSupplier> width)
		{
			this.width = checkNotNull(width);
			return this;
		}

		@Override
		public Builder height(Function<GuiShape, IntSupplier> height)
		{
			this.height = checkNotNull(height);
			return this;
		}

		public Builder iconSize()
		{
			width(o -> () -> o.getIcon()
							  .width());
			height(o -> () -> o.getIcon()
							   .height());
			return this;
		}

		/**
		 * Whether the {@link GuiShape} position will be relative to the component {@link IOffset}. If set to true (default), the
		 * GuiShape position will match the component regardless of the scrolling offset.
		 *
		 * @param fixed true to ignore offset, false otherwise
		 * @return this builder
		 */
		public Builder fixed(boolean fixed)
		{
			this.fixed = fixed;
			return this;
		}

		public Builder zIndex(int z)
		{
			zIndex = () -> z;
			return this;
		}

		public Builder zIndex(IntSupplier supplier)
		{
			zIndex = checkNotNull(supplier);
			return this;
		}

		//COLOR
		public Builder color(int c)
		{
			return color((fp, vp) -> c);
		}

		public Builder color(IntSupplier supplier)
		{
			checkNotNull(supplier);
			return color((fp, vp) -> supplier.getAsInt());
		}

		public Builder color(ToIntBiFunction<FacePosition, VertexPosition> func)
		{
			checkNotNull(func);
			color = colorFunction(func);
			return this;
		}

		//per vertex colors
		private void putColor(VertexPosition position, int color)
		{
			colors.put(position, color);
			color((f, v) -> colors.getOrDefault(v, 0xFFFFFF));
		}

		public Builder color(VertexPosition position, int color)
		{
			putColor(position, color);
			return this;
		}

		public Builder topColor(int color)
		{
			putColor(VertexPosition.TOPLEFT, color);
			putColor(VertexPosition.TOPRIGHT, color);
			return this;
		}

		public Builder bottomColor(int color)
		{
			putColor(VertexPosition.BOTTOMLEFT, color);
			putColor(VertexPosition.BOTTOMRIGHT, color);
			return this;
		}

		public Builder leftColor(int color)
		{
			putColor(VertexPosition.TOPLEFT, color);
			putColor(VertexPosition.BOTTOMLEFT, color);
			return this;
		}

		public Builder rightColor(int color)
		{
			putColor(VertexPosition.TOPRIGHT, color);
			putColor(VertexPosition.BOTTOMRIGHT, color);
			return this;
		}

		//ALPHA
		public Builder alpha(int a)
		{
			alpha = alphaFunction((fp, vp) -> a);
			return this;
		}

		public Builder alpha(IntSupplier supplier)
		{
			checkNotNull(supplier);
			alpha = alphaFunction((fp, vp) -> supplier.getAsInt());
			return this;
		}

		public Builder alpha(ToIntBiFunction<FacePosition, VertexPosition> func)
		{
			checkNotNull(func);
			alpha = alphaFunction(func);
			return this;
		}

		//per vertex alpha
		private void putAlpha(VertexPosition position, int alpha)
		{
			alphas.put(position, alpha);
			alpha((f, v) -> alphas.getOrDefault(v, 0xFFFFFF));
		}

		public Builder alphaFor(VertexPosition position, int alpha)
		{
			putAlpha(position, alpha);
			return this;
		}

		public Builder topAlpha(int alpha)
		{
			putAlpha(VertexPosition.TOPLEFT, alpha);
			putAlpha(VertexPosition.TOPRIGHT, alpha);
			return this;
		}

		public Builder bottomAlpha(int alpha)
		{
			putAlpha(VertexPosition.BOTTOMLEFT, alpha);
			putAlpha(VertexPosition.BOTTOMRIGHT, alpha);
			return this;
		}

		public Builder leftAlpha(int alpha)
		{
			putAlpha(VertexPosition.TOPLEFT, alpha);
			putAlpha(VertexPosition.BOTTOMLEFT, alpha);
			return this;
		}

		public Builder rightAlpha(int alpha)
		{
			putAlpha(VertexPosition.TOPRIGHT, alpha);
			putAlpha(VertexPosition.BOTTOMRIGHT, alpha);
			return this;
		}

		//BORDER
		public Builder border(int size)
		{
			borderSize = size;
			return this;
		}

		public Builder border(int size, int color, int alpha)
		{
			borderColor = color;
			borderSize = size;
			borderAlpha = alpha;
			if (icon == null)
				icon(GuiIcon.NONE);

			this.color = colorFunction(this.color);
			this.alpha = alphaFunction(this.alpha);

			return this;
		}

		public Builder border(int size, int color)
		{
			return border(size, color, 255);
		}

		private ToIntBiFunction<FacePosition, VertexPosition> colorFunction(ToIntBiFunction<FacePosition, VertexPosition> color)
		{
			if (borderSize == 0)
				return color;
			return (fp, vp) -> fp != FacePosition.CENTER ? borderColor : color.applyAsInt(fp, vp);
		}

		private ToIntBiFunction<FacePosition, VertexPosition> alphaFunction(ToIntBiFunction<FacePosition, VertexPosition> alpha)
		{
			if (borderSize == 0)
				return alpha;
			return (fp, vp) -> fp != FacePosition.CENTER ? borderAlpha : alpha.applyAsInt(fp, vp);
		}

		public Builder icon(String name)
		{
			return icon(Theme.icon(name));
		}

		public Builder icon(GuiIcon i)
		{
			icon = () -> i;
			return this;
		}

		public Builder icon(Supplier<GuiIcon> supplier)
		{
			icon = checkNotNull(supplier);
			return this;
		}

		public Builder icon(UIComponent component, String icon, String hover, String disabled)
		{
			return icon(component, Theme.icon(icon), Theme.icon(hover), Theme.icon(disabled));
		}

		public Builder icon(UIComponent component, GuiIcon icon, GuiIcon hover, GuiIcon disabled)
		{
			return icon(GuiIcon.forComponent(component, icon, hover, disabled));
		}

		public GuiShape build()
		{
			return new GuiShape(parent, position, zIndex, size, color, alpha, icon, borderSize, fixed);
		}
	}
}