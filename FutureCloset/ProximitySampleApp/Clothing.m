//
//  Clothing.m
//  ProximitySampleApp-withFrameworks
//
//  Created by Jon Carroll on 1/4/14.
//  Copyright (c) 2014 Qualcomm Retail Solutions, Inc. All rights reserved.
//

#import "Clothing.h"

@implementation Clothing

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
            break;
        case Sweater:
            self.warmth = 8;
            break;
        case Pant:
            self.warmth = 8;
            break;
        case Short:
            self.warmth = 3;
            break;
        case Jacket:
            self.warmth = 10;
            break;
        case Accessory:
            self.warmth = 5;
            break;
        case LongSleeveShirt:
            self.warmth = 7;
            break;
        case Skirt:
            self.warmth = 3;
            break;
        case Dress:
            self.warmth = 4;
            break;
    }
}





@end
