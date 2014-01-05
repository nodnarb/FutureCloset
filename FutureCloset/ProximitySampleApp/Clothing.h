//
//  Clothing.h
//  ProximitySampleApp-withFrameworks
//
//  Created by Jon Carroll on 1/4/14.
//  Copyright (c) 2014 Qualcomm Retail Solutions, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

typedef enum {
    Green,
    Red,
    Blue,
    Black,
    White,
    Purple,
    Orange,
    Brown,
    Pink,
    Yellow
} ClothingColor;

typedef enum {
    TShirt,
    Sweater,
    Pant,
    Short,
    Jacket,
    Accessory,
    LongSleeveShirt,
    Skirt,
    Dress
} ClothingType;

@interface Clothing : NSObject {
    
}

@property (nonatomic) NSString *name;
@property (nonatomic) ClothingColor color;
@property (nonatomic) ClothingType type;
@property (nonatomic) int warmth;
@property (nonatomic) UIImage *picture;
@property (nonatomic) BOOL dirty;



@end