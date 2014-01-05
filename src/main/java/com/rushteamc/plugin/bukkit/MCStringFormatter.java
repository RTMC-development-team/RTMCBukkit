package com.rushteamc.plugin.bukkit;

import java.awt.Color;
import java.util.List;

import org.bukkit.ChatColor;

import com.rushteamc.plugin.common.FormattedString.StringFormatter;
import com.rushteamc.plugin.common.FormattedString.FormattedString.FormattedStringPiece;
import com.rushteamc.plugin.common.FormattedString.FormattedString.ParseErrorException;
import com.rushteamc.plugin.common.FormattedString.FormattedString.Style;

public class MCStringFormatter implements StringFormatter
{
	private static final Style defaultStyle = new Style(Color.white, false, false, false, false, false);

	public void append(List<FormattedStringPiece> formattedStringPieces, String text) throws ParseErrorException
	{
		if(text.equals(""))
			return;
		
		int i = 0;
		int startPiece = 0;
		boolean foundFormatter = false;
		Style style = new Style();
		while(i < text.length())
		{
			if(text.charAt(i) == ChatColor.COLOR_CHAR)
			{
				foundFormatter = true;
				i++;
				continue;
			}
			
			if(foundFormatter)
			{
				foundFormatter = false;
				
				if(startPiece < i-1)
				{
					FormattedStringPiece formattedStringPiece = new FormattedStringPiece(style, text.substring(startPiece, i - 1));
					formattedStringPieces.add(formattedStringPiece);
					style = new Style();
				}
				
				ChatColor chatColor = ChatColor.getByChar(text.charAt(i));
				
				if(chatColor.isColor())
				{
					style.setColor(getColor(chatColor));
				}
				else
				{
					switch(chatColor)
					{
					case BOLD:
						style.setBold(true);
						break;
					case ITALIC:
						style.setItalic(true);
						break;
					case UNDERLINE:
						style.setUnderline(true);
						break;
					case STRIKETHROUGH:
						style.setStrikeThrough(true);
						break;
					case MAGIC:
						style.setBold(true);
						break;
					case RESET:
						style.setBold(false);
						style.setItalic(false);
						style.setUnderline(false);
						style.setStrikeThrough(false);
						style.setRandom(false);
						break;
					default:
						;
					}
				}
				
				i++;
				startPiece = i;
				continue;
			}
			
			i++;
		}
		FormattedStringPiece formattedStringPiece = new FormattedStringPiece(style, text.substring(startPiece, i));
		formattedStringPieces.add(formattedStringPiece);
		/*
		if(startPiece < i - 1)
		{
			FormattedStringPiece formattedStringPiece = new FormattedStringPiece(style, text.substring(startPiece, i));
			formattedStringPieces.add(formattedStringPiece);
		}
		*/
	}
	
	private Color getColor(ChatColor color)
	{
		switch(color)
		{
		case BLACK:
			return new Color(0, 0, 0);
		case DARK_BLUE:
			return (new Color(0, 0, 191));
		case DARK_GREEN:
			return (new Color(0, 191, 0));
		case DARK_AQUA:
			return (new Color(0, 191, 191));
		case DARK_RED:
			return (new Color(191, 0, 0));
		case DARK_PURPLE:
			return (new Color(191, 0, 191));
		case GOLD:
			return (new Color(191, 191, 0));
		case GRAY:
			return (new Color(191, 191, 191));
		case DARK_GRAY:
			return (new Color(40, 40, 40));
		case BLUE:
			return (new Color(64, 64, 255));
		case GREEN:
			return (new Color(64, 255, 64));
		case AQUA:
			return (new Color(64, 255, 255));
		case RED:
			return (new Color(255, 64, 64));
		case LIGHT_PURPLE:
			return (new Color(255, 64, 255));
		case YELLOW:
			return (new Color(255, 255, 64));
		case WHITE:
			return (new Color(255, 255, 255));
		default:
			return null;
		}
	}

	public String getFormattedString(List<FormattedStringPiece> formattedStringPieces) 
	{
		Style currentStyle = defaultStyle;
		StringBuilder stringBuilder = new StringBuilder();
		for( FormattedStringPiece formattedStringPiece : formattedStringPieces )
		{
			Style style = formattedStringPiece.getStyle();
			
			if(style.getColor() != null)
			{
				stringBuilder.append(ChatColor.COLOR_CHAR);

				int coldat = style.getColor().getRGB();
				int err = Integer.MAX_VALUE;
				ChatColor endCol = null;
				for( ChatColor chatColor : ChatColor.values() )
				{
					if(chatColor.isColor())
					{
						Color col = getColor(chatColor);
						int thisError = 0;
						int dat1 = coldat;
						int dat2 = col.getRGB();
						for(int i = 0; i < 3; i++)
						{
							int diff = (dat1 & 0xff) - (dat2 & 0xff);
							thisError += diff * diff;
							dat1 >>= 8;
							dat2 >>= 8;
						}
						if(thisError < err)
						{
							err = thisError;
							endCol = chatColor;
						}
					}
				}
				
				stringBuilder.append(endCol.getChar());
			}
			
			if(style.getBold() != null)
				if(style.getBold() && !currentStyle.getBold())
				{
					stringBuilder.append(ChatColor.COLOR_CHAR);
					stringBuilder.append(ChatColor.BOLD);
					currentStyle.setBold(true);
				}
				else if(!style.getBold() && currentStyle.getBold())
				{
					stringBuilder.append(ChatColor.COLOR_CHAR);
					stringBuilder.append(ChatColor.RESET);
					currentStyle.setBold(false);
					printCurrent(stringBuilder, currentStyle);
				}
			
			if(style.getItalic() != null)
				if(style.getItalic() && !currentStyle.getItalic())
				{
					stringBuilder.append(ChatColor.COLOR_CHAR);
					stringBuilder.append(ChatColor.ITALIC);
					currentStyle.setItalic(true);
				}
				else if(!style.getItalic() && currentStyle.getItalic())
				{
					stringBuilder.append(ChatColor.COLOR_CHAR);
					stringBuilder.append(ChatColor.RESET);
					currentStyle.setItalic(false);
					printCurrent(stringBuilder, currentStyle);
				}
			
			if(style.getUnderline() != null)
				if(style.getUnderline() && !currentStyle.getUnderline())
				{
					stringBuilder.append(ChatColor.COLOR_CHAR);
					stringBuilder.append(ChatColor.UNDERLINE);
					currentStyle.setUnderline(true);
				}
				else if(!style.getUnderline() && currentStyle.getUnderline())
				{
					stringBuilder.append(ChatColor.COLOR_CHAR);
					stringBuilder.append(ChatColor.RESET);
					currentStyle.setUnderline(false);
					printCurrent(stringBuilder, currentStyle);
				}
			
			if(style.getStrikeThrough() != null)
				if(style.getStrikeThrough() && !currentStyle.getStrikeThrough())
				{
					stringBuilder.append(ChatColor.COLOR_CHAR);
					stringBuilder.append(ChatColor.STRIKETHROUGH);
					currentStyle.setStrikeThrough(true);
				}
				else if(!style.getStrikeThrough() && currentStyle.getStrikeThrough())
				{
					stringBuilder.append(ChatColor.COLOR_CHAR);
					stringBuilder.append(ChatColor.RESET);
					currentStyle.setStrikeThrough(false);
					printCurrent(stringBuilder, currentStyle);
				}
			
			if(style.getRandom() != null)
				if(style.getRandom() && !currentStyle.getRandom())
				{
					stringBuilder.append(ChatColor.COLOR_CHAR);
					stringBuilder.append(ChatColor.MAGIC);
					currentStyle.setRandom(true);
				}
				else if(!style.getRandom() && currentStyle.getRandom())
				{
					stringBuilder.append(ChatColor.COLOR_CHAR);
					stringBuilder.append(ChatColor.RESET);
					currentStyle.setRandom(false);
					printCurrent(stringBuilder, currentStyle);
				}
			
			stringBuilder.append(formattedStringPiece.getText());
		}
		
		return stringBuilder.toString();
	}
	
	private void printCurrent(StringBuilder stringBuilder, Style style)
	{
		if(style.getBold())
		{
			stringBuilder.append('&');
			stringBuilder.append('g');
		}

		if(style.getItalic())
		{
			stringBuilder.append('&');
			stringBuilder.append('h');
		}
	}
}
