package net.malisis.ego.gui.text;

import net.malisis.ego.font.EGOFont;
import net.malisis.ego.font.FontOptions.FontOptionsBuilder;
import net.minecraft.util.text.TextFormatting;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public interface IFontOptionsBuilder<BUILDER, COMPONENT>
{

	BUILDER self();

	/**
	 * @return the actual {@link FontOptionsBuilder} used in the builder implementing {@link IFontOptionsBuilder}
	 */
	FontOptionsBuilder fob();

	/**
	 * {@link FontOptionsBuilder#when(Predicate)} returns a different instance than the base builder.<br>
	 * Implementation of the {@link IFontOptionsBuilder} interface overwrite the builder that will be returned by {@link FontOptionsBuilder#when(Predicate)}
	 * <code>
	 * BUILDER when(Predicate<COMPONENT> predicate);
	 * {
	 * fontOptionsBuilder = fob().when(predicate);
	 * return self();
	 * }
	 * </code>
	 *
	 * @param predicate
	 * @return
	 */
	BUILDER when(Predicate<COMPONENT> predicate);

	default BUILDER font(EGOFont font)
	{
		fob().font(font);
		return self();
	}

	default BUILDER scale(float scale)
	{
		fob().scale(scale);
		return self();
	}

	default BUILDER textColor(int color)
	{
		fob().color(color);
		return self();
	}

	default BUILDER textColor(TextFormatting color)
	{
		fob().color(color);
		return self();
	}

	default BUILDER bold()
	{
		return bold(true);
	}

	default BUILDER bold(boolean bold)
	{
		fob().bold(bold);
		return self();
	}

	default BUILDER italic()
	{
		return italic(true);
	}

	default BUILDER italic(boolean italic)
	{
		fob().italic(italic);
		return self();
	}

	default BUILDER underline()
	{
		return underline(true);
	}

	default BUILDER underline(boolean underline)
	{
		fob().underline(underline);
		return self();
	}

	default BUILDER strikethrough()
	{
		return strikethrough(true);
	}

	default BUILDER strikethrough(boolean strikethrough)
	{
		fob().strikethrough(strikethrough);
		return self();
	}

	default BUILDER obfuscated()
	{
		return obfuscated(true);
	}

	default BUILDER obfuscated(boolean obfuscated)
	{
		fob().obfuscated(obfuscated);
		return self();
	}

	default BUILDER obfuscatedCharList(String charList)
	{
		fob().obfuscatedCharList(charList);
		return self();
	}

	default BUILDER shadow()
	{
		return shadow(true);
	}

	default BUILDER shadow(boolean shadow)
	{
		fob().shadow(shadow);
		return self();
	}

	default BUILDER lineSpacing(int spacing)
	{
		fob().lineSpacing(spacing);
		return self();
	}

	default BUILDER textRightAligned()
	{
		fob().rightAligned();
		return self();
	}

	default BUILDER TextLeftAligned()
	{
		fob().leftAligned();
		return self();
	}

	default BUILDER styles(String styles)
	{
		fob().styles(styles);
		return self();
	}

	default BUILDER styles(TextFormatting... formats)
	{
		fob().styles(formats);
		return self();
	}

	default BUILDER when(BooleanSupplier supplier)
	{
		return when(o -> supplier.getAsBoolean());
	}

}
