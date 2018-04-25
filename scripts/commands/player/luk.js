function start() {
	if (args.length != 2) {
		c.getPlayer().showMessage("@luk <amount>");
		return;
	}
	var amount = parseInt(args[1]);
	if (amount > 0) {
		if (amount > c.getPlayer().getRemainingAp()) {
			c.getPlayer().showMessage("You do not have enough AP.");
		} else if (amount + c.getPlayer().getStat().getLuk() > 32767) {
			c.getPlayer().showMessage("You can increase your LUK to a maximum of 32767.");
		} else {
			c.getPlayer().getStat().setLuk(c.getPlayer().getStat().getLuk() + amount, c.getPlayer());
			c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - amount);
		}
	} else if (amount < 0) {
		if (-amount > c.getPlayer().getStat().getLuk() - 4) {
			c.getPlayer().showMessage("You can decrease your LUK to a minimum of 4.");
		} else {
			c.getPlayer().getStat().setLuk(c.getPlayer().getStat().getLuk() + amount, c.getPlayer());
			c.getPlayer().setRemainingAp(c.getPlayer().getRemainingAp() - amount);
		}
	}
}