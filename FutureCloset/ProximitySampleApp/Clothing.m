//
//  Clothing.m
//  ProximitySampleApp-withFrameworks
//
//  Created by Jon Carroll on 1/4/14.
//  Copyright (c) 2014 Qualcomm Retail Solutions, Inc. All rights reserved.
//

#import "Clothing.h"

static NSMutableArray *theClothes = nil;

@implementation Clothing

+(NSMutableArray*)getClothes {
    if(theClothes==nil) {
        theClothes = [[NSMutableArray alloc] init];
    }
    return theClothes;
}

-(id)init {
    self = [super init];
    
    self.name = @"Not Set";
    self.color = White;
    self.type = TShirt;
    self.warmth = 5;
    self.picture = [UIImage imageNamed:@"TShirt.png"];
    self.dirty = NO;
    
    return self;
}

-(void)setDefaultsForType:(ClothingType)t {
    self.type = t;
    
    switch (t) {
        case TShirt:
            self.warmth = 4;
            self.picture = [UIImage imageNamed:@"TShirt.png"];
            break;
        case Sweater:
            self.warmth = 8;
            self.picture = [UIImage imageNamed:@"Sweater.png"];
            break;
        case Pant:
            self.warmth = 8;
            self.picture = [UIImage imageNamed:@"Pant.png"];
            break;
        case Short:
            self.warmth = 3;
            self.picture = [UIImage imageNamed:@"Short.png"];
            break;
        case Jacket:
            self.warmth = 10;
            self.picture = [UIImage imageNamed:@"Jacket.png"];
            break;
        case Accessory:
            self.warmth = 5;
            self.picture = [UIImage imageNamed:@"Accessory.png"];
            break;
        case LongSleeveShirt:
            self.warmth = 7;
            self.picture = [UIImage imageNamed:@"LongSleeveShirt.png"];
            break;
        case Skirt:
            self.warmth = 3;
            self.picture = [UIImage imageNamed:@"Skirt.png"];
            break;
        case Dress:
            self.warmth = 4;
            self.picture = [UIImage imageNamed:@"Dress.png"];
            break;
    }
}

-(NSString*)getTypeAsString {
    NSString *name = @"Unknown";
    switch (self.type) {
        case TShirt:
            name = @"T-Shirt";
            break;
        case Sweater:
            name = @"Sweater";
            break;
        case Pant:
            name = @"Pant";
            break;
        case Short:
            name = @"Short";
            break;
        case Jacket:
            name = @"Jacket";
            break;
        case Accessory:
            name = @"Accessory";
            break;
        case LongSleeveShirt:
            name = @"Long Sleeve Shirt";
            break;
        case Skirt:
            name = @"Skirt";
            break;
        case Dress:
            name = @"Dress";
            break;
        default:
            break;
    }
    return name;
}


-(void)setTransmitter:(Transmitter *)transmitter {
    _transmitter = transmitter;
    
    if([transmitter.name isEqualToString:@"Beacon 1"]) {
        self.name = @"Generic Gray T-Shirt";
        self.color = Black;
        [self setDefaultsForType:TShirt];
    }
    
    if([transmitter.name isEqualToString:@"Beacon 2"]) {
        self.name = @"Hoodie";
        self.color = Black;
        [self setDefaultsForType:Jacket];
    }
    
    if([transmitter.name isEqualToString:@"Beacon 3"]) {
        self.name = @"U MAD BRO?";
        self.color = Black;
        [self setDefaultsForType:T-Shirt];
    }

    if([transmitter.name isEqualToString:@"Beacon 4"]) {
        self.name = @"Ugly Christmas Sweater";
        self.color = Green;
        [self setDefaultsForType:Sweater]
    }

    if([transmitter.name isEqualToString:@"Beacon 5"]) {
        self.name = @"Orange Sweater";
        self.color = Orange;
        [self setDefaultsForType:Sweater];
    }

    if([transmitter.name isEqualToString:@"Beacon 6"]) {
        self.name = @"Pokemon Master Hat";
        self.color = Yellow;
        [self setDefaultsForType:Accessory];
    }

    if([transmitter.name isEqualToString:@"Beacon 7"]) {
        self.name = @"Ratty Shirt";
        self.color = Orange;
        [self setDefaultsForType:TShirt];
    }

    if([transmitter.name isEqualToString:@"Beacon 8"]) {
        self.name = @"Cynthia's Panties";
        self.color = Purple;
        [self setDefaultsForType:Short];
    }

    if([transmitter.name isEqualToString:@"Beacon 9"]) {
        self.name = @"Lucy's Panties";
        self.color = Pink;
        [self setDefaultsForType:Short];
    }

    if([transmitter.name isEqualToString:@"Beacon 10"]) {
        self.name = @"Hero of Time Tunic";
        self.color = Green;
        [self setDefaultsForType:LongSleeveShirt];
    }

    if([transmitter.name isEqualToString:@"Beacon 11"]) {
        self.name = @"Jeans";
        self.color = Black;
        [self setDefaultsForType:Pant];
    }

    if([transmitter.name isEqualToString:@"Beacon 12"]) {
        self.name = @"Tight Red Pants";
        self.color = Red;
        [self setDefaultsForType:Pant];
    }

    if([transmitter.name isEqualToString:@"Beacon 13"]) {
        self.name = @"BATMAN JAMMIES!!!";
        self.color = Black;
        [self setDefaultsForType:Dress];
    }

    if([transmitter.name isEqualToString:@"Beacon 14"]) {
        self.name = @"Scarf";
        self.color = Red;
        [self setDefaultsForType:Accessory];
    }

    [self updateImageForColor];
}

-(void)updateImageForColor {
    UIColor *color = [[UIColor alloc] initWithRed:0.0f green:0.0f blue:0.0f alpha:1.0f];
    switch(self.color) {
        case Green:
            color = [UIColor greenColor];
            break;
        case Blue:
            color = [UIColor blueColor];
            break;
        case Red:
            color = [UIColor redColor];
            break;
        case White:
            color = [UIColor whiteColor];
            break;
        case Purple:
            color = [UIColor purpleColor];
            break;
        case Orange:
            color = [UIColor orangeColor];
            break;
        case Brown:
            color = [UIColor brownColor];
            break;
        case Pink:
            color = [UIColor redColor];
            break;
        case Yellow:
            color = [UIColor yellowColor];
            break;
        default:
            break;
    }
    
    self.picture = [self changeColor:self.picture toColor:color];
}

-(UIImage*)changeColor:(UIImage*)image toColor:(UIColor*)color {
	CGRect contextRect;
	contextRect.origin.x = 0.0f;
	contextRect.origin.y = 0.0f;
	contextRect.size = [image size];
	// Retrieve source image and begin image context
	CGSize itemImageSize = [image size];
	CGPoint itemImagePosition;
	itemImagePosition.x = ceilf((contextRect.size.width - itemImageSize.width) / 2);
	itemImagePosition.y = ceilf((contextRect.size.height - itemImageSize.height) );
	UIGraphicsBeginImageContext(contextRect.size);
	CGContextRef c = UIGraphicsGetCurrentContext();
	// Setup shadow
	// Setup transparency layer and clip to mask
	CGContextBeginTransparencyLayer(c, NULL);
	CGContextScaleCTM(c, 1.0, -1.0);
	CGContextClipToMask(c, CGRectMake(itemImagePosition.x, -itemImagePosition.y, itemImageSize.width, -itemImageSize.height), [image CGImage]);
	// Fill and end the transparency layer
	const float* colors = CGColorGetComponents( color.CGColor );
	CGContextSetRGBFillColor(c, colors[0], colors[1], colors[2], colors[3]);
	contextRect.size.height = -contextRect.size.height;
	contextRect.size.height -= 15;
	CGContextFillRect(c, contextRect);
	CGContextEndTransparencyLayer(c);
	UIImage *img = UIGraphicsGetImageFromCurrentImageContext();
	UIGraphicsEndImageContext();
	return img;
}




@end
