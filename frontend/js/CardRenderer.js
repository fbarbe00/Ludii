// frontend/js/CardRenderer.js

class CardRenderer {
    /**
     * @param {object} cardData - e.g., { id: "c1", faceUp: true, type: "standard_52", value: "KH" }
     * @param {object} properties - from gameState.pieceProperties[cardData.value] or gameState.cardProperties[cardData.value]
     *                              e.g., { imageUrl: "path/to/king_hearts.png", color: 0xFFFFFF, backColor: 0x0000FF,
     *                                     suitColor: 0xFF0000, rankText: "K", suitText: "♥" }
     * @param {PIXI.Application} app
     */
    constructor(cardData, properties, app) {
        this.cardData = cardData;
        this.props = properties || {}; // Ensure props is an object
        this.app = app;
        this.container = new PIXI.Container();
        this.graphics = new PIXI.Graphics();
        this.container.addChild(this.graphics);
    }

    /**
     * @param {number} x - X position of the card's top-left corner
     * @param {number} y - Y position of the card's top-left corner
     * @param {number} width - Width of the card
     * @param {number} height - Height of the card
     */
    draw(x, y, width, height) {
        this.container.position.set(x, y);
        this.graphics.clear();

        const cardColor = this.props.color || 0xFFFFFF; // Default white face
        const cardBackColor = this.props.backColor || 0x0000FF; // Default blue back
        const strokeColor = this.props.strokeColor || 0x000000;
        const cornerRadius = this.props.cornerRadius || 5;

        // Card Body
        this.graphics.beginFill(this.cardData.faceUp ? cardColor : cardBackColor);
        this.graphics.lineStyle(1, strokeColor, 1);
        this.graphics.drawRoundedRect(0, 0, width, height, cornerRadius);
        this.graphics.endFill();

        // Content (Image or Text)
        if (this.cardData.faceUp) {
            if (this.props.imageUrl) {
                try {
                    // Check if sprite already exists, update texture, else create new
                    if (!this.sprite || this.sprite.texture.label !== this.props.imageUrl) {
                        if (this.sprite) this.sprite.destroy();
                        const texture = PIXI.Texture.from(this.props.imageUrl);
                        this.sprite = new PIXI.Sprite(texture);
                    }
                    this.sprite.width = width * 0.9; // Leave some padding
                    this.sprite.height = height * 0.9;
                    this.sprite.anchor.set(0.5);
                    this.sprite.position.set(width / 2, height / 2);
                    this.container.addChild(this.sprite);
                } catch (e) {
                    console.error("Error loading card texture:", this.props.imageUrl, e);
                    this._drawFaceText(width, height); // Fallback
                }
            } else {
                this._drawFaceText(width, height);
            }
        } else {
            // Draw a simple card back pattern if no image for back
            if (!this.props.imageUrl) { // Only draw pattern if no image was attempted for back
                this.graphics.lineStyle(2, strokeColor, 0.7);
                this.graphics.moveTo(width * 0.1, height * 0.1);
                this.graphics.lineTo(width * 0.9, height * 0.9);
                this.graphics.moveTo(width * 0.9, height * 0.1);
                this.graphics.lineTo(width * 0.1, height * 0.9);
            }
        }
        return this.container;
    }

    _drawFaceText(width, height) {
        // Simple text rendering for card face if no image
        const rank = this.props.rankText || this.cardData.value.charAt(0) || "?";
        const suit = this.props.suitText || (this.cardData.value.length > 1 ? this.cardData.value.charAt(1) : "") || "";

        let suitColor = this.props.suitColor;
        if (!suitColor) {
            suitColor = (suit === '♥' || suit === '♦' || suit.toUpperCase() === 'H' || suit.toUpperCase() === 'D') ? 0xFF0000 : 0x000000;
        }

        const textStyle = new PIXI.TextStyle({
            fontFamily: 'Arial',
            fontSize: Math.min(width, height) / 3,
            fill: suitColor,
            align: 'center'
        });

        const rankLabel = new PIXI.Text(rank, textStyle);
        rankLabel.anchor.set(0.5);
        rankLabel.position.set(width / 2, height / 2);
        this.container.addChild(rankLabel);

        // You could add smaller suit symbols in corners too for a more card-like look
        const suitLabel = new PIXI.Text(suit, { ...textStyle, fontSize: Math.min(width, height) / 5 });
        suitLabel.anchor.set(0,0);
        suitLabel.position.set(width * 0.1, height * 0.05);
        this.container.addChild(suitLabel);
    }

    getDisplayObject() {
        return this.container;
    }
}

window.CardRenderer = CardRenderer;
