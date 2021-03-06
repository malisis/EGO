/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Ordinastie
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
package net.malisis.ego.gui.text;

import static com.google.common.base.Preconditions.*;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.malisis.ego.cacheddata.CachedData;
import net.malisis.ego.cacheddata.CachedFontOptions;
import net.malisis.ego.cacheddata.FixedData;
import net.malisis.ego.cacheddata.ICachedData;
import net.malisis.ego.cacheddata.IntCachedData;
import net.malisis.ego.font.FontOptions;
import net.malisis.ego.font.StringWalker;
import net.malisis.ego.gui.EGOGui;
import net.malisis.ego.gui.component.UIComponent;
import net.malisis.ego.gui.component.content.IContent;
import net.malisis.ego.gui.element.IClipable;
import net.malisis.ego.gui.element.IClipable.ClipArea;
import net.malisis.ego.gui.element.position.IPositionBuilder;
import net.malisis.ego.gui.element.position.Position;
import net.malisis.ego.gui.element.position.Position.IPosition;
import net.malisis.ego.gui.element.position.Positions;
import net.malisis.ego.gui.element.size.Size;
import net.malisis.ego.gui.element.size.Size.ISize;
import net.malisis.ego.gui.element.size.Size.ISized;
import net.malisis.ego.gui.render.GuiRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

/**
 * The Class GuiString represents a String to be used and displayed in a GUI.<br>
 * It can be translated, and used with fixed or dynamic named parameters.<br>
 * Parameters are used with {key} markers in the text.<br>
 * The text will be automatically split in lines based on the wrap size.
 *
 * @author Ordinastie
 */
public class GuiText implements IContent
{
	public static boolean CACHED = true;

	/** Pattern for named parameters. */
	private static final Pattern pattern = Pattern.compile("\\{(?<key>.*?)}");

	/** Base text to be translated and parameterized. */
	private CachedData<String> base;
	/** Lines composing the text. */
	private final List<LineInfo> lines = Lists.newArrayList();
	/** Parameters. */
	private final Map<String, ICachedData<?>> parameters;

	/** The base font options. */
	private FontOptions defaultOptions;
	/** The current font options. Only different from defaultOptions if fit size is set and require changing the text scale to fit */
	private FontOptions fitOptions;

	/** Translated text with resolved parameters. */
	private String cache = "";

	/** Whether the text should be translated. */
	private final boolean translated;
	/** Whether the text should handle styles. */
	private final boolean literal;

	/** Text wrap size. 0 means do not wrap. */
	private IntCachedData wrapSize;
	private final IntCachedData fitSize;

	private final CachedFontOptions cachedOptions = new CachedFontOptions(this::getFontOptions);

	private IPosition position;
	private final boolean fixed;
	private final IPosition screenPosition = new Position.ScreenPosition(this);
	private ISize size;
	private final IntSupplier zIndex;
	private final IntSupplier alpha;

	//private boolean buildLines = true;
	//private boolean buildCache = true;

	private ISized parent;

	private GuiText(Builder builder)
	{
		base = new CachedData<>(builder.base);
		parameters = Maps.newHashMap(builder.parameters);
		position = builder.position.apply(this);
		fixed = builder.fixed;
		zIndex = builder.zIndex;
		alpha = builder.alpha;
		parent = builder.parent;
		defaultOptions = builder.fontOptions;
		fitOptions = builder.fontOptions;
		translated = builder.translated;
		literal = builder.literal;
		wrapSize = new IntCachedData(builder.wrapSize);
		fitSize = new IntCachedData(builder.fitSize);

		//	update(true);
	}

	public void setParent(ISized parent)
	{
		this.parent = parent;
	}

	@Override
	public ISized getParent()
	{
		return parent;
	}

	public void setPosition(IPosition position)
	{
		this.position = position;
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

	/**
	 * Gets the size of the text.<br>
	 * Width matches longest line, height is the sum of each line height
	 *
	 * @return the i size
	 */
	@Nonnull
	@Override
	public ISize size()
	{
		update(); //make sure to update cache
		return size;
	}

	/**
	 * Gets the raw text of this {@link GuiText}.
	 *
	 * @return the raw text
	 */
	public String getBase()
	{
		return base.get();
	}

	/**
	 * Gets processed text, translated and with resolved parameters.
	 *
	 * @return the text
	 */
	public String getText()
	{
		update();
		return cache;
	}

	/**
	 * Sets the text for this {@link GuiText}.<br>
	 * Generates the cache and resolves parameters.
	 *
	 * @param text the new text
	 */
	public void setText(String text)
	{
		checkNotNull(text);
		setText(() -> text);
	}

	public void setText(Supplier<String> supplier)
	{
		base = new CachedData<>(checkNotNull(supplier));
	}

	public void setParameters(Map<String, ICachedData<?>> params)
	{
		parameters.clear();
		parameters.putAll(checkNotNull(params));
	}

	public void setWrapSize(int size)
	{
		wrapSize = new IntCachedData(size);
	}

	public void setWrapSize(IntSupplier supplier)
	{
		wrapSize = new IntCachedData(checkNotNull(supplier));
	}

	/**
	 * Gets the different lines.
	 *
	 * @return the list
	 */
	public List<LineInfo> lines()
	{
		update();
		return lines;
	}

	public int length()
	{
		update();
		return cache.length();
	}

	public boolean isEmpty()
	{
		return length() <= 0;
	}

	/**
	 * Gets the line count of this text.
	 *
	 * @return the int
	 */
	public int lineCount()
	{
		return lines.size();
	}

	/**
	 * Checks if the text should be translated.
	 *
	 * @return true, if is translated
	 */
	public boolean isTranslated()
	{
		return translated;
	}

	/**
	 * Checks if the text should be rendered without styles.
	 *
	 * @return true, if is literal
	 */
	public boolean isLiteral()
	{
		return literal;
	}

	/**
	 * Gets the wrap size for the text.
	 *
	 * @return the wrap size
	 */
	public int getWrapSize()
	{
		return wrapSize.get();
	}

	/**
	 * Gets the font options used to render.
	 *
	 * @return the font options
	 */
	public FontOptions getFontOptions()
	{
		return defaultOptions;
	}

	/**
	 * Sets the font options to use to render.
	 *
	 * @param fontOptions the new font options
	 */
	public void setFontOptions(FontOptions fontOptions)
	{
		defaultOptions = checkNotNull(fontOptions);
	}

	/**
	 * Resolve parameter values to use in the text.
	 *
	 * @param key the key
	 * @return the string
	 */
	private String resolveParameter(String key)
	{
		String str = Optional.ofNullable(parameters.get(key))
							 .map(d -> Objects.toString(d.get()))
							 .orElse(key);
		return translate(str).replace("$", "\\$");
	}

	private String translate(String key)
	{
		return translated ? I18n.format(key) : key;
	}

	/**
	 * Checks whether any parameter has changed.
	 *
	 * @return true, if successful
	 */
	private boolean hasParametersChanged()
	{
		boolean changed = false;
		for (ICachedData<?> data : parameters.values())
		{
			data.update();
			if (data.hasChanged())
				changed = true; //can't return early, we need to call update on each data
		}
		return changed;
	}

	/**
	 * Update the cache and the lines if necessary.
	 */
	private void update()
	{
		base.update();
		wrapSize.update();
		fitSize.update();
		cachedOptions.update();

		//check params first as they need to be updated
		boolean buildCache = hasParametersChanged() || base.hasChanged();
		//check fontOptions first, as last values need to be updated
		boolean buildLines = cachedOptions.hasChanged() || buildCache || fitSize.hasChanged() || wrapSize.hasChanged();

		if (buildCache || !CACHED)
			generateCache();

		if (buildLines || !CACHED)
		{
			buildLines(defaultOptions);
			if (checkFitSize())
				buildLines(fitOptions);
		}
	}

	private void updateSize(FontOptions options)
	{
		int w = 0, h = 0;
		for (LineInfo info : lines)
		{
			w = Math.max(info.width(), w);
			h += info.height() + options.lineSpacing();
		}
		size = Size.of(w, h);

		for (LineInfo info : lines)
		{
			info.spaceWidth = w;
		}
	}

	/**
	 * Generates the text cache and rebuild the lines.<br>
	 * Translates and applies the parameters.
	 */
	private void generateCache()
	{
		String str = base.get();
		str = applyParameters(str);
		cache = str;
	}

	/**
	 * Applies parameters to the text.
	 *
	 * @param str the str
	 * @return the string
	 */
	public String applyParameters(String str)
	{
		if (Strings.isEmpty(str))
			return "";
		Matcher matcher = pattern.matcher(str);
		StringBuffer sb = new StringBuffer();
		while (matcher.find())
		{
			matcher.appendReplacement(sb, resolveParameter(matcher.group("key")));
		}
		matcher.appendTail(sb);
		str = sb.toString();
		return translate(str);
	}

	/**
	 * Splits the cache in multiple lines to fit in the {@link #wrapSize}.
	 * Is called once with the default scale, and once again if fitSize requires new scale
	 */
	private void buildLines(FontOptions fontOptions)
	{
		lines.clear();

		String str = cache.replace("\r?(?<=\n)", "\n");

		StringBuilder line = new StringBuilder();
		StringBuilder word = new StringBuilder();
		//FontRenderOptions fro = new FontRenderOptions();

		int wrapWidth = getWrapSize();
		float lineWidth = 0;
		float wordWidth = 0;
		float lineHeight = 0;

		StringWalker walker = new StringWalker(str, fontOptions);
		walker.skipChars(false);
		walker.applyStyles(true);
		while (walker.walk())
		{
			char c = walker.getChar();
			lineWidth += walker.width();
			wordWidth += walker.width();
			lineHeight = Math.max(lineHeight, walker.height());

			word.append(c);

			//we just ended a new word, add it to the current line
			if (Character.isWhitespace(c) || c == '-' || c == '.')
			{
				line.append(word);
				word.setLength(0);
				wordWidth = 0;
			}
			if ((wrapWidth > 0 && lineWidth >= wrapWidth) || c == '\n')
			{
				//the first word on the line is too large, split anyway
				if (line.length() == 0)
				{
					line.append(word);
					word.setLength(0);
					wordWidth = 0;
				}

				//remove current word from last line
				lineWidth -= wordWidth;

				//add the new line
				lines.add(new LineInfo(line.toString(), MathHelper.ceil(lineWidth), MathHelper.ceil(lineHeight), 0));
				line.setLength(0);

				lineWidth = wordWidth;
			}
		}

		line.append(word);
		lines.add(new LineInfo(line.toString(), MathHelper.ceil(lineWidth), MathHelper.ceil(lineHeight), 0));

		updateSize(fontOptions);
	}

	private boolean checkFitSize()
	{
		fitOptions = defaultOptions;
		int fitWidth = fitSize.get();
		if (fitWidth <= 0)
			return false;

		float factor = EGOGui.current()
							 .scaleFactor();

		//text already fits
		if (size.width() <= fitWidth) //don't call size() (size->update->checkFitSize)
			return false;

		float scale = (float) fitWidth / size.width();
		//adapt the scaling to the GUI resolution factor to prevent squished letters
		scale = (float) Math.floor(scale * factor) / factor;

		if (scale == defaultOptions.getFontScale()) //should never happen ?
			return false;

		fitOptions = defaultOptions.toBuilder()
								   .scale(scale)
								   .build();
		return true;

	}

	/**
	 * Renders all the text based on its set position.
	 *
	 * @param renderer the renderer
	 */
	@Override
	public void render(GuiRenderer renderer)
	{
		update();
		if (StringUtils.isEmpty(cache))
			return;

		int x = screenPosition.x();
		int y = screenPosition.y();
		int z = zIndex.getAsInt();
		int a = alpha.getAsInt();
		ClipArea area = null;
		if (parent instanceof IClipable)
			area = ((IClipable) parent).getClipArea();
		render(renderer, x, y, z, a, area);
	}

	/**
	 * Renders lines between startLine and endLine of this {@link GuiText} at the coordinates.
	 *
	 * @param renderer the renderer
	 * @param x the x
	 * @param y the y
	 * @param z the z
	 * @param area the area
	 */
	public void render(GuiRenderer renderer, int x, int y, int z, int alpha, ClipArea area)
	{
		if (StringUtils.isEmpty(cache))
			return;

		fitOptions.getFont()
				  .render(renderer, this, x, y, z, alpha, fitOptions, area);

	}

	/**
	 * Creates and returns a {@link StringWalker} for this text.
	 *
	 * @return the string walker
	 */
	public StringWalker walker()
	{
		return new StringWalker(this, defaultOptions);
	}

	@Override
	public String toString()
	{
		String str = lines.size() > 0 ? lines.get(0)
											 .text()
											 .replace("\n", "") : "";
		str += " " + position + "@" + size;
		if (wrapSize.get() != 0)
			str += " (wrap: " + wrapSize.get() + ")";
		return str;
		//return lines().stream().map(LineInfo::getText).collect(Collectors.joining());
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static GuiText of(String text)
	{
		return new Builder().text(text)
							.build();
	}

	public static GuiText of(String text, FontOptions options)
	{
		return new Builder().text(text)
							.fontOptions(options)
							.build();
	}

	public static class Builder implements IPositionBuilder<Builder, GuiText>
	{
		private Supplier<String> base = () -> "";
		private final Map<String, ICachedData<?>> parameters = Maps.newHashMap();
		private FontOptions fontOptions = FontOptions.EMPTY;
		private Function<GuiText, IntSupplier> x = gt -> Positions.leftAligned(gt, 0);
		private Function<GuiText, IntSupplier> y = gt -> Positions.topAligned(gt, 0);
		private Function<GuiText, IPosition> position = gt -> Position.of(x.apply(gt), y.apply(gt));

		private IntSupplier zIndex = () -> 0;
		private IntSupplier alpha = () -> 255;
		private ISized parent;
		private boolean translated = true;
		private boolean literal = false;
		private IntSupplier wrapSize;
		private IntSupplier fitSize;
		private boolean fixed;

		protected Builder()
		{
		}

		public Builder text(String text)
		{
			return text(() -> text != null ? text : "");
		}

		public Builder text(Supplier<String> supplier)
		{
			base = checkNotNull(supplier);
			return this;
		}

		@Override
		public Builder position(Function<GuiText, IPosition> func)
		{
			position = checkNotNull(func);
			return this;
		}

		@Override
		public Builder x(Function<GuiText, IntSupplier> x)
		{
			this.x = checkNotNull(x);
			return this;
		}

		@Override
		public Builder y(Function<GuiText, IntSupplier> y)
		{
			this.y = checkNotNull(y);
			return this;
		}

		public Builder fixed()
		{
			fixed = true;
			return this;
		}

		public Builder zIndex(int zIndex)
		{
			return zIndex(() -> zIndex);
		}

		public Builder zIndex(IntSupplier zIndex)
		{
			this.zIndex = checkNotNull(zIndex);
			return this;
		}

		public Builder alpha(int alpha)
		{
			return alpha(() -> alpha);
		}

		public Builder alpha(IntSupplier alpha)
		{
			this.alpha = checkNotNull(alpha);
			return this;
		}

		public Builder parent(ISized parent)
		{
			this.parent = parent;
			if (parent instanceof UIComponent)
			{
				alpha(((UIComponent) parent)::getAlpha);
				zIndex(((UIComponent) parent)::zIndex);
			}
			return this;
		}

		/**
		 * Binds a fixed value to the specified parameter.
		 *
		 * @param <T> the generic type
		 * @param key the key
		 * @param value the value
		 */
		public <T> Builder bind(String key, T value)
		{
			return bind(key, new FixedData<>(value));
		}

		/**
		 * Binds supplier to the specified parameter.
		 *
		 * @param <T> the generic type
		 * @param key the key
		 * @param supplier the supplier
		 */
		public <T> Builder bind(String key, Supplier<T> supplier)
		{
			return bind(key, new CachedData<>(supplier));
		}

		/**
		 * Binds {@link ICachedData} to the specified parameter.
		 *
		 * @param <T> the generic type
		 * @param key the key
		 * @param data the data
		 */
		public <T> Builder bind(String key, ICachedData<T> data)
		{
			parameters.put(key, data);
			return this;
		}

		public Builder fontOptions(FontOptions fontOptions)
		{
			this.fontOptions = fontOptions;
			return this;
		}

		public Builder translated(boolean translated)
		{
			this.translated = translated;
			return this;
		}

		public Builder literal(boolean literal)
		{
			this.literal = literal;
			return this;
		}

		public Builder literal()
		{
			literal = true;
			return this;
		}

		public Builder wrapSize(int size)
		{
			wrapSize = () -> size;
			return this;
		}

		public Builder wrapSize(IntSupplier supplier)
		{
			wrapSize = checkNotNull(supplier);
			return this;
		}

		public Builder fitSize(int size)
		{
			fitSize = () -> size;
			return this;
		}

		public Builder fitSize(IntSupplier supplier)
		{
			fitSize = checkNotNull(supplier);
			return this;
		}

		public GuiText build()
		{
			return new GuiText(this);
		}

	}

	public static class LineInfo implements ISize
	{
		private final String text;
		private final int width;
		private final int height;
		private float spaceWidth;

		private LineInfo(String text, int width, int height, float spaceWidth)
		{
			this.text = text;
			this.width = width;
			this.height = height;
			this.spaceWidth = spaceWidth;
		}

		public String text()
		{
			return text;
		}

		@Override
		public int width()
		{
			return width;
		}

		@Override
		public int height()
		{
			return height;
		}

		public float spaceWidth()
		{
			return spaceWidth;
		}
	}

}
